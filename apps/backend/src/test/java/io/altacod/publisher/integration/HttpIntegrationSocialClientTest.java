package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.altacod.publisher.config.IntegrationAiProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class HttpIntegrationSocialClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void publishTelegramSendsTextAndAttachmentsToIntegrationService() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> apiKeyHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/social/posts", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKeyHeader.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            byte[] response = """
                    {"status":"success","providerPostId":"telegram-message-1"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            IntegrationAiProperties props = props(server.getAddress().getPort());
            HttpIntegrationSocialClient client = new HttpIntegrationSocialClient(props, objectMapper);

            String id = client.publishTelegram(
                    "ws-1-post-2",
                    "bot-token",
                    "chat-1",
                    "caption",
                    true,
                    List.of(new HttpIntegrationSocialClient.BinaryAttachment(
                            "image",
                            "photo.jpg",
                            "image/jpeg",
                            "hello".getBytes(StandardCharsets.UTF_8),
                            null
                    ))
            );

            JsonNode root = objectMapper.readTree(requestBody.get());
            assertThat(id).isEqualTo("telegram-message-1");
            assertThat(apiKeyHeader.get()).isEqualTo("aikey_test");
            assertThat(root.path("platform").asText()).isEqualTo("telegram");
            assertThat(root.path("text").asText()).isEqualTo("caption");
            assertThat(root.path("credentials").path("botToken").asText()).isEqualTo("bot-token");
            assertThat(root.path("credentials").path("chatId").asText()).isEqualTo("chat-1");
            assertThat(root.path("attachments")).hasSize(1);
            assertThat(root.path("attachments").get(0).path("base64").asText()).isEqualTo("aGVsbG8=");
            assertThat(root.path("options").path("disableWebPagePreview").asBoolean()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    private IntegrationAiProperties props(int port) throws IOException {
        IntegrationAiProperties props = new IntegrationAiProperties();
        props.setBaseUrl("http://127.0.0.1:" + port);
        props.setApiKey("aikey_test");
        props.setConnectTimeoutMs(1_000);
        props.setReadTimeoutMs(5_000);
        return props;
    }
}
