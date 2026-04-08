package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.LoginRequest;
import io.altacod.publisher.api.dto.RegisterRequest;
import io.altacod.publisher.api.dto.TokenResponse;
import io.altacod.publisher.common.Slugify;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

        String token = jwtService.createAccessToken(user.getEmail());
        return TokenResponse.bearer(token);
    }

    public TokenResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        var token = new UsernamePasswordAuthenticationToken(email, request.password());
        authenticationManager.authenticate(token);
        String jwt = jwtService.createAccessToken(email);
        return TokenResponse.bearer(jwt);
    }
}
