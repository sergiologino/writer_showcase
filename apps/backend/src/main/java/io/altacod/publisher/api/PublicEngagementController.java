package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.PublicEngagementDto;
import io.altacod.publisher.security.SecurityUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/w/{workspaceSlug}/posts")
public class PublicEngagementController {

    private final PostEngagementService postEngagementService;

    public PublicEngagementController(PostEngagementService postEngagementService) {
        this.postEngagementService = postEngagementService;
    }

    @GetMapping("/{postSlug}/engagement")
    public PublicEngagementDto engagement(
            @PathVariable String workspaceSlug,
            @PathVariable String postSlug,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return postEngagementService.getEngagement(workspaceSlug, postSlug, userId);
    }
}
