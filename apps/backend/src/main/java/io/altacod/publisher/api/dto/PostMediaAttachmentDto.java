package io.altacod.publisher.api.dto;

public record PostMediaAttachmentDto(
        long mediaAssetId,
        String mimeType,
        String altText,
        int sortOrder,
        String caption
) {
}
