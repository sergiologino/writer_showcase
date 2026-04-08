package io.altacod.publisher.api.dto;

import java.time.Instant;

public record PublicPostSummaryDto(
        long id,
        String title,
        String slug,
        String excerpt,
        Instant publishedAt
) {
}
