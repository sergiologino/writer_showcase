package io.altacod.publisher.api.dto;

import java.time.Instant;

public record CategoryResponse(
        long id,
        String name,
        String slug,
        String description,
        String color,
        Instant createdAt,
        Instant updatedAt
) {
}
