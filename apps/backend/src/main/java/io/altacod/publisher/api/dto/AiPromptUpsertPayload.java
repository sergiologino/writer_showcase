package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiPromptUpsertPayload(
        String title,
        @Size(max = 50_000) String systemPrompt,
        @Size(max = 100_000) String userPromptTemplate
) {
}
