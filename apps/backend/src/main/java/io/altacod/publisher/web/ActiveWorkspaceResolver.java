package io.altacod.publisher.web;

import io.altacod.publisher.security.SecurityUserPrincipal;
import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.MembershipRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ActiveWorkspaceResolver implements HandlerMethodArgumentResolver {

    public static final String HEADER = "X-Workspace-Id";

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public ActiveWorkspaceResolver(UserRepository userRepository, MembershipRepository membershipRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ActiveWorkspace.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        UserEntity user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String raw = request != null ? request.getHeader(HEADER) : null;
        if (raw != null && !raw.isBlank()) {
            long workspaceId;
            try {
                workspaceId = Long.parseLong(raw.trim());
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workspace header");
            }
            if (!membershipRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace not accessible");
            }
            return workspaceId;
        }

        var memberships = membershipRepository.findByUserOrderByIdAsc(user);
        if (memberships.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No workspace for user");
        }
        return memberships.get(0).getWorkspace().getId();
    }
}
