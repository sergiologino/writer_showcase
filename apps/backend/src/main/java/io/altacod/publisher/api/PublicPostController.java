package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.PublicPostDetailDto;
import io.altacod.publisher.api.dto.PublicPostSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/w/{workspaceSlug}/posts")
public class PublicPostController {

    private final PublicPostService publicPostService;

    public PublicPostController(PublicPostService publicPostService) {
        this.publicPostService = publicPostService;
    }

    @GetMapping
    public Page<PublicPostSummaryDto> list(
            @PathVariable String workspaceSlug,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return publicPostService.listPublished(workspaceSlug, pageable);
    }

    @GetMapping("/{postSlug}")
    public PublicPostDetailDto get(@PathVariable String workspaceSlug, @PathVariable String postSlug) {
        return publicPostService.getPublished(workspaceSlug, postSlug);
    }
}
