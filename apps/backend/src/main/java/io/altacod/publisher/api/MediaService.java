package io.altacod.publisher.api;

import io.altacod.publisher.api.dto.MediaAssetResponse;
import io.altacod.publisher.config.PublisherStorageProperties;
import io.altacod.publisher.media.MediaAssetEntity;
import io.altacod.publisher.media.MediaAssetRepository;
import io.altacod.publisher.media.MediaSourceType;
import io.altacod.publisher.media.MediaType;
import io.altacod.publisher.media.PostMediaRepository;
import io.altacod.publisher.storage.LocalObjectStorage;
import io.altacod.publisher.workspace.WorkspaceEntity;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Instant;

@Service
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final WorkspaceRepository workspaceRepository;
    private final LocalObjectStorage objectStorage;
    private final PublisherStorageProperties storageProperties;
    private final PostMediaRepository postMediaRepository;

    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            WorkspaceRepository workspaceRepository,
            LocalObjectStorage objectStorage,
            PublisherStorageProperties storageProperties,
            PostMediaRepository postMediaRepository
    ) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectStorage = objectStorage;
        this.storageProperties = storageProperties;
        this.postMediaRepository = postMediaRepository;
    }

    @Transactional
    public MediaAssetResponse upload(Long workspaceId, MultipartFile file, String altText) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (file.getSize() > storageProperties.getMaxUploadBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large");
        }
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        String storageKey;
        try {
            storageKey = objectStorage.store(workspaceId, file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage failed");
        }

        String mime = file.getContentType();
        MediaType type = mapMediaType(mime);
        Instant now = Instant.now();
        String trimmedAlt = altText == null || altText.isBlank() ? null : altText.trim();
        var entity = new MediaAssetEntity(
                workspace,
                type,
                MediaSourceType.UPLOAD,
                storageKey,
                mime,
                file.getSize(),
                trimmedAlt,
                now
        );
        mediaAssetRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Page<MediaAssetResponse> list(Long workspaceId, Pageable pageable) {
        return mediaAssetRepository
                .findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void delete(Long workspaceId, Long id) {
        MediaAssetEntity asset = mediaAssetRepository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        try {
            objectStorage.deleteIfExists(asset.getStorageKey());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot delete file");
        }
        mediaAssetRepository.delete(asset);
    }

    @Transactional(readOnly = true)
    public MediaFileView fileForWorkspace(Long workspaceId, Long mediaId) {
        MediaAssetEntity asset = mediaAssetRepository.findByIdAndWorkspaceId(mediaId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        try {
            Path p = objectStorage.resolveExisting(asset.getStorageKey());
            Resource resource = new UrlResource(p.toUri());
            String ct = asset.getMimeType() == null ? "application/octet-stream" : asset.getMimeType();
            return new MediaFileView(resource, ct);
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid file URL");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File missing");
        }
    }

    @Transactional(readOnly = true)
    public MediaFileView mediaFileForPublicDownload(String workspaceSlug, Long mediaId) {
        if (postMediaRepository.countPublicPublishedByMediaAndWorkspaceSlug(mediaId, workspaceSlug) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        MediaAssetEntity asset = mediaAssetRepository.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        if (!asset.getWorkspace().getSlug().equals(workspaceSlug)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        try {
            Path p = objectStorage.resolveExisting(asset.getStorageKey());
            Resource resource = new UrlResource(p.toUri());
            return new MediaFileView(resource, asset.getMimeType() == null ? "application/octet-stream" : asset.getMimeType());
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid file URL");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File missing");
        }
    }

    private MediaAssetResponse toResponse(MediaAssetEntity e) {
        return new MediaAssetResponse(
                e.getId(),
                e.getType(),
                e.getSourceType(),
                e.getMimeType(),
                e.getSizeBytes(),
                e.getAltText(),
                e.getCreatedAt()
        );
    }

    private static MediaType mapMediaType(String mime) {
        if (mime == null) {
            return MediaType.FILE;
        }
        if (mime.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (mime.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (mime.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        return MediaType.FILE;
    }

    public record MediaFileView(Resource resource, String contentType) {
    }
}
