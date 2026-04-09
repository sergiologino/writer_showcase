package io.altacod.publisher.api.dto;

import io.altacod.publisher.media.MediaSourceType;
import io.altacod.publisher.media.MediaType;

import java.time.Instant;

public record MediaAssetResponse(
        long id,
        MediaType type,
        MediaSourceType sourceType,
        String mimeType,
        Long sizeBytes,
        String altText,
        Instant createdAt
) {
}
