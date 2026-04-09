package io.altacod.publisher.api;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/w/{workspaceSlug}/media")
public class PublicMediaController {

    private final MediaService mediaService;

    public PublicMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping("/{mediaId}/file")
    public ResponseEntity<Resource> file(@PathVariable String workspaceSlug, @PathVariable Long mediaId) {
        MediaService.MediaFileView view = mediaService.mediaFileForPublicDownload(workspaceSlug, mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(view.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(view.resource());
    }
}
