package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotNull;

public record ChannelUpsertPayload(
        @NotNull Boolean enabled,
        String label,
        @NotNull String configJson
) {
}
