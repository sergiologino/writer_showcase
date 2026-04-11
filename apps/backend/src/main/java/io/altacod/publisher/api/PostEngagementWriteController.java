package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.PostCommentPayload;
import io.altacod.publisher.api.dto.PublicEngagementDto;
import io.altacod.publisher.security.SecurityUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/engagement/w/{workspaceSlug}/posts/{postSlug}")
public class PostEngagementWriteController {

    private final PostEngagementService postEngagementService;

    public PostEngagementWriteController(PostEngagementService postEngagementService) {
        this.postEngagementService = postEngagementService;
    }

    @PostMapping("/likes")
    public PublicEngagementDto toggleLike(
            @PathVariable String workspaceSlug,
            @PathVariable String postSlug,
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return postEngagementService.toggleLike(workspaceSlug, postSlug, principal.getId());
    }

    @PostMapping("/comments")
    public PublicEngagementDto addComment(
            @PathVariable String workspaceSlug,
            @PathVariable String postSlug,
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody PostCommentPayload payload
    ) {
        return postEngagementService.addComment(workspaceSlug, postSlug, principal.getId(), payload.body());
    }
}
