package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagPayload(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String slug
) {
}
