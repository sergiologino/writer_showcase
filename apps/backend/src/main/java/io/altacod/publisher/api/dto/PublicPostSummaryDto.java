package io.altacod.publisher.api.dto;

import java.time.Instant;

public record PublicPostSummaryDto(
        long id,
        String title,
        String slug,
        String excerpt,
        Instant publishedAt,
        /** Первое вложение поста (для миниатюры в списке). */
        Long firstMediaId,
        String firstMediaUrl,
        String firstMediaMimeType,
        /** Обрезанный plain-text из HTML тела (для превью в списке). */
        String bodyPreviewPlain,
        String authorDisplayName,
        /** Публичный URL аватара автора или null. */
        String authorAvatarUrl
) {
}
