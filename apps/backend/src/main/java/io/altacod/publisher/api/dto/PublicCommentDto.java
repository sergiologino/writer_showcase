package io.altacod.publisher.api.dto;

import java.time.Instant;

public record PublicCommentDto(
        long id,
        String authorDisplayName,
        String body,
        Instant createdAt
) {
}
