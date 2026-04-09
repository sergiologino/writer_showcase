package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfilePayload(
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 32) String locale,
        @Size(max = 64) String timezone,
        @Pattern(regexp = "light|dark|system", message = "theme must be light, dark, or system")
        String theme
) {
}
