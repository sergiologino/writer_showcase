package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Достаёт человекочитаемый текст из тела ответа noteapp-ai-integration (оболочка {@code AiResponseDTO}
 * с полем {@code response}) и из вложенных JSON нейросетей (OpenAI-совместимый и др.).
 */
public final class AiIntegrationTextExtractor {

    private AiIntegrationTextExtractor() {
    }

    /**
     * Только для {@code status=success}: разбор {@code response} и {@code tokensUsed}.
     */
    public static Parsed parseSuccessBody(String body, ObjectMapper objectMapper) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        Integer tokens = null;
        if (root.hasNonNull("tokensUsed") && root.get("tokensUsed").isNumber()) {
            tokens = root.get("tokensUsed").asInt();
        }
        String text = extractFromResponseNode(root.get("response"), objectMapper);
        return new Parsed(text, tokens);
    }

    private static String extractFromResponseNode(JsonNode responseNode, ObjectMapper objectMapper) {
        if (responseNode == null || responseNode.isNull()) {
            return "";
        }
        if (responseNode.isTextual()) {
            String s = responseNode.asText();
            if (s != null && !s.isBlank() && (s.trim().startsWith("{") || s.trim().startsWith("["))) {
                try {
                    return extractFromResponseNode(objectMapper.readTree(s), objectMapper);
                } catch (Exception ignored) {
                    return s.trim();
                }
            }
            return s != null ? s.trim() : "";
        }
        if (!responseNode.isObject()) {
            return responseNode.asText("");
        }
        JsonNode choices = responseNode.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode msg = choices.get(0).path("message");
            if (msg.hasNonNull("content")) {
                return msg.get("content").asText("").trim();
            }
        }
        for (String key : new String[] {"content", "text", "output"}) {
            JsonNode n = responseNode.get(key);
            if (n != null && n.isTextual()) {
                return n.asText("").trim();
            }
        }
        if (responseNode.hasNonNull("imageUrl")) {
            return responseNode.get("imageUrl").asText("");
        }
        if (responseNode.hasNonNull("imageBase64")) {
            return "[Изображение сгенерировано]";
        }
        if (responseNode.hasNonNull("videoUrl")) {
            return responseNode.get("videoUrl").asText("");
        }
        if (responseNode.hasNonNull("audioBase64")) {
            return "[Аудио]";
        }
        return responseNode.toString();
    }

    public record Parsed(String displayText, Integer tokensUsed) {
    }
}
