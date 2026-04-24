package io.altacod.publisher.api.dto;

import java.util.List;
import java.util.Map;

/**
 * Ключи — {@code requestType} (chat, image_generation, …), значения — имена сетей по убыванию приоритета.
 */
public record AiRoutingPayload(Map<String, List<String>> routing) {
}
