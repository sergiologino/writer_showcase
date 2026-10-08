package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.altacod.publisher.config.IntegrationAiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpIntegrationAiClientTest {

    @Test
    void availableNetworksUsesClientApiKeyAndReturnsArray() throws Exception {
        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> bearer = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/ai/networks/available", exchange -> {
            key.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            bearer.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "[{\"name\":\"test-network\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String result = client(props(server.getAddress().getPort())).fetchAvailableNetworksJson();
            assertThat(result).isEqualTo("[{\"name\":\"test-network\"}]");
            assertThat(key.get()).isEqualTo("aikey_test");
            assertThat(bearer.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void upstreamFailureIsReportedInsteadOfEmptyList() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/ai/networks/available", exchange -> {
            exchange.sendResponseHeaders(502, -1);
            exchange.close();
        });
        server.start();
        try {
            assertThatThrownBy(() -> client(props(server.getAddress().getPort())).fetchAvailableNetworksJson())
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(error -> {
                        ResponseStatusException ex = (ResponseStatusException) error;
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                        assertThat(ex.getReason()).contains("HTTP 502");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void missingConfigurationIsReportedInsteadOfEmptyList() {
        assertThatThrownBy(() -> client(new IntegrationAiProperties()).fetchAvailableNetworksJson())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException ex = (ResponseStatusException) error;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
    }

    private HttpIntegrationAiClient client(IntegrationAiProperties props) {
        return new HttpIntegrationAiClient(props, new ObjectMapper());
    }

    private IntegrationAiProperties props(int port) {
        IntegrationAiProperties props = new IntegrationAiProperties();
        props.setBaseUrl("http://127.0.0.1:" + port);
        props.setApiKey("aikey_test");
        return props;
    }
}
