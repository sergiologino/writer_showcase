package io.altacod.publisher.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.altacod.publisher.config.IntegrationAiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Публикация в Telegram, Facebook и X через noteapp-ai-integration ({@code POST /api/social/posts}).
 * См. {@code noteapp-ai-integration/docs/ai/EXTERNAL_SERVICES_INTEGRATION.md}.
 */
@Component
public class HttpIntegrationSocialClient {

    private final IntegrationAiProperties props;
    private final ObjectMapper objectMapper;

    public HttpIntegrationSocialClient(IntegrationAiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /**
     * @return идентификатор поста у провайдера, если сервис вернул его; иначе null
     */
    public String publish(
            String userId,
            String platform,
            String text,
            Map<String, String> credentials,
            Map<String, Object> options,
            List<AttachmentPayload> attachments
    ) {
        if (!props.isConfigured()) {
            throw new IllegalStateException("Noteapp integration is not configured (publisher.integration-ai.base-url and api-key)");
        }
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        rf.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        RestClient client = RestClient.builder()
                .requestFactory(rf)
                .baseUrl(props.getBaseUrl().replaceAll("/$", ""))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(props.getApiKeyHeader(), props.getApiKey())
                .build();
        String path = props.getSocialPostsPath().startsWith("/")
                ? props.getSocialPostsPath()
                : "/" + props.getSocialPostsPath();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("userId", userId);
        body.put("platform", platform);
        body.put("text", text);
        ObjectNode cred = body.putObject("credentials");
        credentials.forEach(cred::put);
        if (options != null && !options.isEmpty()) {
            ObjectNode opt = body.putObject("options");
            for (Map.Entry<String, Object> e : options.entrySet()) {
                opt.set(e.getKey(), objectMapper.valueToTree(e.getValue()));
            }
        }
        if (attachments != null && !attachments.isEmpty()) {
            body.set("attachments", objectMapper.valueToTree(attachments));
        }
        final String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Social API: failed to build JSON body", e);
        }
        String raw;
        try {
            raw = client.post()
                    .uri(path)
                    .body(jsonBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Social API HTTP " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RestClientException ex) {
            throw new IllegalStateException("Social API: " + ex.getMessage(), ex);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(raw == null ? "{}" : raw);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Social API: invalid JSON response", e);
        }
        String status = root.path("status").asText("");
        if (!"success".equalsIgnoreCase(status)) {
            String err = root.path("errorMessage").asText("unknown error");
            throw new IllegalStateException("Social API: " + err);
        }
        JsonNode id = root.path("providerPostId");
        return id.isMissingNode() || id.isNull() ? null : id.asText();
    }

    public String publish(
            String userId,
            String platform,
            String text,
            Map<String, String> credentials,
            Map<String, Object> options
    ) {
        return publish(userId, platform, text, credentials, options, null);
    }

    public String publishTelegramText(String userId, String botToken, String chatId, String text, boolean disableWebPagePreview) {
        Map<String, String> c = new HashMap<>();
        c.put("botToken", botToken);
        c.put("chatId", chatId);
        Map<String, Object> opt = new HashMap<>();
        opt.put("disableWebPagePreview", disableWebPagePreview);
        return publish(userId, "telegram", text, c, opt);
    }

    public String publishTelegram(
            String userId,
            String botToken,
            String chatId,
            String text,
            boolean disableWebPagePreview,
            List<BinaryAttachment> attachments
    ) {
        Map<String, String> c = new HashMap<>();
        c.put("botToken", botToken);
        c.put("chatId", chatId);
        Map<String, Object> opt = new HashMap<>();
        opt.put("disableWebPagePreview", disableWebPagePreview);
        List<AttachmentPayload> payloads = new ArrayList<>();
        if (attachments != null) {
            for (BinaryAttachment attachment : attachments) {
                payloads.add(AttachmentPayload.from(attachment));
            }
        }
        return publish(userId, "telegram", text, c, opt, payloads);
    }

    public String publishFacebook(
            String userId,
            String accessToken,
            String pageId,
            String text,
            String linkOrNull
    ) {
        Map<String, String> c = new HashMap<>();
        c.put("accessToken", accessToken);
        c.put("pageId", pageId);
        Map<String, Object> opt = new HashMap<>();
        if (linkOrNull != null && !linkOrNull.isBlank()) {
            opt.put("link", linkOrNull);
        }
        return publish(userId, "facebook", text, c, opt);
    }

    public String publishX(String userId, String bearerToken, String text) {
        Map<String, String> c = new HashMap<>();
        c.put("bearerToken", bearerToken);
        return publish(userId, "x", text, c, null);
    }

    public record BinaryAttachment(String type, String fileName, String contentType, byte[] data, String caption) {
    }

    public record AttachmentPayload(String type, String fileName, String contentType, String base64, String caption) {

        static AttachmentPayload from(BinaryAttachment attachment) {
            return new AttachmentPayload(
                    attachment.type(),
                    attachment.fileName(),
                    attachment.contentType(),
                    Base64.getEncoder().encodeToString(attachment.data()),
                    attachment.caption()
            );
        }
    }
}
