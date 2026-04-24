package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.LoginRequest;
import io.altacod.publisher.api.dto.RefreshRequest;
import io.altacod.publisher.api.dto.RegisterRequest;
import io.altacod.publisher.api.dto.TokenResponse;
import io.altacod.publisher.auth.RefreshTokenEntity;
import io.altacod.publisher.auth.RefreshTokenRepository;
import io.altacod.publisher.common.Slugify;
import io.altacod.publisher.common.TokenHasher;
import io.altacod.publisher.config.PublisherProperties;
import io.altacod.publisher.security.JwtService;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.MembershipEntity;
import io.altacod.publisher.workspace.MembershipRepository;
import io.altacod.publisher.workspace.MembershipRole;
import io.altacod.publisher.workspace.WorkspaceEntity;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PublisherProperties publisherProperties;

    public AuthService(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenRepository refreshTokenRepository,
            PublisherProperties publisherProperties
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.publisherProperties = publisherProperties;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Instant now = Instant.now();
        String display = request.displayName();
        if (display == null || display.isBlank()) {
            display = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        }
        display = display.trim();
        var user = new UserEntity(
                email,
                passwordEncoder.encode(request.password()),
                display,
                null,
                null,
                "system",
                now
        );
        userRepository.save(user);

        String workspaceSlug = Slugify.uniqueSlug(display + "-space", workspaceRepository::existsBySlug);
        var workspace = new WorkspaceEntity(display + " workspace", workspaceSlug, user, now);
        workspaceRepository.save(workspace);

        membershipRepository.save(new MembershipEntity(workspace, user, MembershipRole.OWNER, now));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        var token = new UsernamePasswordAuthenticationToken(email, request.password());
        log.info("AuthService.login: start emailLen={} passwordPresent={}", email.length(), request.password() != null && !request.password().isEmpty());
        try {
            authenticationManager.authenticate(token);
        } catch (AuthenticationException e) {
            // Не пробрасывать AuthenticationException из @RestController: в части сценариев это даёт 403.
            // Явно 401 + тело из ApiExceptionHandler.
            log.warn("AuthService.login: Spring Security authentication failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("AuthService.login: user missing after successful authenticate (emailLen={})", email.length());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                });
        log.info("AuthService.login: success userId={}", user.getId());
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String raw = request.refreshToken().trim();
        String hash = TokenHasher.sha256Hex(raw);
        RefreshTokenEntity existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        Instant now = Instant.now();
        if (!existing.isActive(now)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        UserEntity user = existing.getUser();
        existing.revoke(now);
        refreshTokenRepository.save(existing);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse signInWithOAuth(
            String provider,
            String subject,
            String email,
            String displayName
    ) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth: email is required");
        }
        if (provider == null || provider.isBlank() || subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth: subject or provider is missing");
        }
        email = email.trim().toLowerCase();
        if (displayName == null || displayName.isBlank()) {
            int at = email.indexOf('@');
            displayName = at > 0 ? email.substring(0, at) : email;
        } else {
            displayName = displayName.trim();
        }
        String prov = provider.trim();
        String sub = subject.trim();
        log.info("AuthService.signInWithOAuth: provider={} subLen={} emailLen={}", prov, sub.length(), email.length());
        Optional<UserEntity> byOauth = userRepository.findByOauthProviderAndOauthSubject(prov, sub);
        if (byOauth.isPresent()) {
            UserEntity u = byOauth.get();
            refreshTokenRepository.revokeAllForUser(u.getId(), Instant.now());
            return issueTokens(u);
        }
        Optional<UserEntity> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            UserEntity u = byEmail.get();
            if (u.getOauthProvider() != null && u.getOauthSubject() != null) {
                if (!u.getOauthProvider().equals(prov) || !u.getOauthSubject().equals(sub)) {
                    log.warn("AuthService.signInWithOAuth: OAuth conflict for userId={}", u.getId());
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "This email is already linked to another sign-in");
                }
            }
            u.setOauthProvider(prov);
            u.setOauthSubject(sub);
            u.touch(Instant.now());
            userRepository.save(u);
            refreshTokenRepository.revokeAllForUser(u.getId(), Instant.now());
            return issueTokens(u);
        }
        Instant now = Instant.now();
        var user = new UserEntity(
                email,
                null,
                displayName,
                null,
                null,
                "system",
                now
        );
        user.setOauthProvider(prov);
        user.setOauthSubject(sub);
        userRepository.save(user);
        String workspaceSlug = Slugify.uniqueSlug(displayName + "-space", workspaceRepository::existsBySlug);
        var workspace = new WorkspaceEntity(displayName + " workspace", workspaceSlug, user, now);
        workspaceRepository.save(workspace);
        membershipRepository.save(new MembershipEntity(workspace, user, MembershipRole.OWNER, now));
        return issueTokens(user);
    }

    private TokenResponse issueTokens(UserEntity user) {
        String access = jwtService.createAccessToken(user.getEmail());
        String refresh = persistRefreshToken(user);
        return TokenResponse.bearer(access, refresh, jwtService.accessTtlSeconds());
    }

    private String persistRefreshToken(UserEntity user) {
        String raw = TokenHasher.newOpaqueToken(32);
        String hash = TokenHasher.sha256Hex(raw);
        int days = publisherProperties.jwt().refreshTtlDays();
        Instant exp = Instant.now().plus(days, ChronoUnit.DAYS);
        refreshTokenRepository.save(new RefreshTokenEntity(user, hash, exp, Instant.now()));
        return raw;
    }
}
