package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.PageResponse;
import io.altacod.publisher.api.dto.PostPayload;
import io.altacod.publisher.api.dto.PostResponse;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.security.SecurityUserPrincipal;
import io.altacod.publisher.web.ActiveWorkspace;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(
            @ActiveWorkspace Long workspaceId,
            @AuthenticationPrincipal SecurityUserPrincipal principal,
            @Valid @RequestBody PostPayload payload
    ) {
        return postService.create(workspaceId, principal.getId(), payload);
    }

    @GetMapping
    public PageResponse<PostResponse> list(
            @ActiveWorkspace Long workspaceId,
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(postService.list(workspaceId, status, q, pageable));
    }

    @GetMapping("/{id}")
    public PostResponse get(@ActiveWorkspace Long workspaceId, @PathVariable Long id) {
        return postService.get(workspaceId, id);
    }

    @PutMapping("/{id}")
    public PostResponse update(
            @ActiveWorkspace Long workspaceId,
            @PathVariable Long id,
            @Valid @RequestBody PostPayload payload
    ) {
        return postService.update(workspaceId, id, payload);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@ActiveWorkspace Long workspaceId, @PathVariable Long id) {
        postService.delete(workspaceId, id);
    }
}
