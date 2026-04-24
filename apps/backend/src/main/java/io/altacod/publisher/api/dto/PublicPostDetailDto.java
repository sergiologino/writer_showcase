package io.altacod.publisher.api.dto;

import java.time.Instant;
import java.util.List;

public record PublicPostDetailDto(
        long id,
        String title,
        String slug,
        String excerpt,
        String bodyHtml,
        CategoryResponse category,
        List<TagSummaryDto> tags,
        List<PublicMediaDto> media,
        Instant publishedAt,
        Instant updatedAt,
        String authorDisplayName,
        String authorAvatarUrl
) {
}
