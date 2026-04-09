package io.altacod.publisher.api.dto;

import java.time.Instant;

public record AiPromptResponse(
        long id,
        String promptKey,
        String title,
        String systemPrompt,
        String userPromptTemplate,
        Instant updatedAt
) {
}
