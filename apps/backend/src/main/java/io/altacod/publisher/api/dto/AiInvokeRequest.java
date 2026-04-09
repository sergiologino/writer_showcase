package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Вызов промпта; в интеграцию уходит {@code AiRequestDTO} (см. noteapp-ai-integration).
 */
public record AiInvokeRequest(
        @NotBlank @Size(max = 64) String promptKey,
        Map<String, String> variables,
        @Size(max = 500_000) String contentSnippet,
        /** {@code userId} во внешнем сервисе; иначе {@code publisher-workspace-{id}}. */
        @Size(max = 256) String externalUserId,
        /** Логическое имя сети в админке интеграции; пусто — автовыбор. */
        @Size(max = 128) String networkName,
        /** Например {@code chat}; по умолчанию {@code chat}. */
        @Size(max = 64) String requestType,
        /** Попадает в {@code metadata} запроса к интеграции. */
        Map<String, String> metadata
) {
}
