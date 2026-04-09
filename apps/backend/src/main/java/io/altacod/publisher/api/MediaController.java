package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.MediaAssetResponse;
import io.altacod.publisher.api.dto.PageResponse;
import io.altacod.publisher.web.ActiveWorkspace;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MediaAssetResponse upload(
            @ActiveWorkspace Long workspaceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText
    ) {
        return mediaService.upload(workspaceId, file, altText);
    }

    @GetMapping
    public PageResponse<MediaAssetResponse> list(
            @ActiveWorkspace Long workspaceId,
            @PageableDefault(size = 24) Pageable pageable
    ) {
        return PageResponse.from(mediaService.list(workspaceId, pageable));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> download(@ActiveWorkspace Long workspaceId, @PathVariable Long id) {
        MediaService.MediaFileView view = mediaService.fileForWorkspace(workspaceId, id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(view.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(view.resource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@ActiveWorkspace Long workspaceId, @PathVariable Long id) {
        mediaService.delete(workspaceId, id);
        return ResponseEntity.noContent().build();
    }
}
