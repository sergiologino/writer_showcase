package io.altacod.publisher.integration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Собирает {@code payload.messages} для {@code requestType = chat}.
 */
public final class AiPromptFormatting {

    private AiPromptFormatting() {
    }

    public static Map<String, Object> chatPayload(
            String systemPrompt,
            String userTemplate,
            Map<String, String> variables,
            String contentSnippet
    ) {
        List<Map<String, String>> messages = new ArrayList<>();
        String sys = systemPrompt == null ? "" : systemPrompt.trim();
        if (!sys.isEmpty()) {
            messages.add(Map.of("role", "system", "content", sys));
        }
        String userContent = applyVariables(userTemplate == null ? "" : userTemplate, variables == null ? Map.of() : variables);
        if (contentSnippet != null && !contentSnippet.isBlank()) {
            if (userContent.isBlank()) {
                userContent = contentSnippet.trim();
            } else {
                userContent = userContent + "\n\n---\n\n" + contentSnippet.trim();
            }
        }
        if (userContent.isBlank()) {
            userContent = sys.isEmpty()
                    ? "."
                    : "Ответь согласно системной инструкции выше.";
        }
        messages.add(Map.of("role", "user", "content", userContent));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messages", messages);
        return payload;
    }

    static String applyVariables(String template, Map<String, String> vars) {
        String s = template;
        for (var e : vars.entrySet()) {
            String v = e.getValue() == null ? "" : e.getValue();
            s = s.replace("{" + e.getKey() + "}", v);
        }
        return s;
    }
}
