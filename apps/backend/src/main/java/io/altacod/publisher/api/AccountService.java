package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.MeResponse;
import io.altacod.publisher.api.dto.UpdateProfilePayload;
import io.altacod.publisher.api.dto.UserSummaryDto;
import io.altacod.publisher.api.dto.WorkspaceSummaryDto;
import io.altacod.publisher.security.SecurityUserPrincipal;
import io.altacod.publisher.config.PublisherSecurityProperties;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PublisherSecurityProperties securityProperties;

    public AccountService(
            UserRepository userRepository,
            MembershipRepository membershipRepository,
            PublisherSecurityProperties securityProperties
    ) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.securityProperties = securityProperties;
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        var memberships = membershipRepository.findByUserOrderByIdAsc(user);
        List<WorkspaceSummaryDto> workspaces = memberships.stream()
                .map(m -> new WorkspaceSummaryDto(
                        m.getWorkspace().getId(),
                        m.getWorkspace().getName(),
                        m.getWorkspace().getSlug(),
                        m.getRole()
                ))
                .toList();

        var userDto = toUserSummary(user);
        return new MeResponse(userDto, workspaces);
    }

    @Transactional
    public MeResponse updateProfile(UpdateProfilePayload payload) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        var user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        user.setDisplayName(payload.displayName().trim());
        user.setLocale(blankToNull(payload.locale()));
        user.setTimezone(blankToNull(payload.timezone()));
        user.setTheme(payload.theme());
        user.touch(Instant.now());
        userRepository.save(user);

        return me();
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private UserSummaryDto toUserSummary(UserEntity user) {
        boolean admin = user.isAdmin() || securityProperties.isListedAdmin(user.getEmail());
        return new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getLocale(),
                user.getTimezone(),
                user.getTheme(),
                admin
        );
    }
}
