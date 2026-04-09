package io.altacod.publisher.integration;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Тело {@code POST /api/ai/process} в noteapp-ai-integration ({@code AiRequestDTO}).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoteappAiProcessRequest(
        String userId,
        String networkName,
        String requestType,
        Map<String, Object> payload,
        Map<String, String> metadata
) {
}
