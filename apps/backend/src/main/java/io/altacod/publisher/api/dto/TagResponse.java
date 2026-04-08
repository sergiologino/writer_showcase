package io.altacod.publisher.api.dto;

import java.time.Instant;

public record TagResponse(long id, String name, String slug, Instant createdAt, Instant updatedAt) {
}
