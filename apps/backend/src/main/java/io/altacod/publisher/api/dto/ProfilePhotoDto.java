package io.altacod.publisher.api.dto;

public record ProfilePhotoDto(
        long mediaAssetId,
        /** Публичный путь, без host; картинки в &lt;img src&gt; без Authorization. */
        String url,
        String mimeType
) {
}
