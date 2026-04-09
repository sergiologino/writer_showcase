package io.altacod.publisher.api.dto;

public record PublicMediaDto(
        long id,
        String url,
        String mimeType,
        String altText,
        int sortOrder,
        String caption
) {
}
