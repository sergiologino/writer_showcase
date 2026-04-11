package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostCommentPayload(
        @NotBlank
        @Size(max = 8000)
        String body
) {
}
