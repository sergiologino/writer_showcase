package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryPayload(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 255) String slug,
        @Size(max = 5000) String description,
        @Size(max = 32) String color
) {
}
