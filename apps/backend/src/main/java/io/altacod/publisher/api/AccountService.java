package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.MeResponse;
import io.altacod.publisher.api.dto.UserSummaryDto;
import io.altacod.publisher.api.dto.WorkspaceSummaryDto;
import io.altacod.publisher.security.SecurityUserPrincipal;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public AccountService(UserRepository userRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
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

        var userDto = new UserSummaryDto(user.getId(), user.getEmail(), user.getDisplayName());
        return new MeResponse(userDto, workspaces);
    }
}
