package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * Произвольный вызов интеграции без шаблона workspace (тело как у {@code AiRequestDTO.payload}).
 */
public record StudioAiRequest(
        @Size(max = 64) String requestType,
        Map<String, Object> payload,
        Map<String, String> metadata,
        @Size(max = 256) String externalUserId,
        @Size(max = 128) String networkName
) {
}
