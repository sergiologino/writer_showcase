package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.api.dto.AiInvokeResponse;
import io.altacod.publisher.config.IntegrationAiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * HTTP-клиент к noteapp-ai-integration: {@code POST /api/ai/process} с {@code X-API-Key} (без Bearer).
 * Документация: {@code noteapp-ai-integration/docs/ai/EXTERNAL_SERVICES_INTEGRATION.md}.
 */
@Component
public class HttpIntegrationAiClient implements IntegrationAiClient {

    private final IntegrationAiProperties props;
    private final ObjectMapper objectMapper;

    public HttpIntegrationAiClient(IntegrationAiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public String fetchAvailableNetworksJson() {
        if (!props.isConfigured()) {
            return "[]";
        }
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        rf.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));
        RestClient client = RestClient.builder()
                .requestFactory(rf)
                .baseUrl(props.getBaseUrl().replaceAll("/$", ""))
                .defaultHeader(props.getApiKeyHeader(), props.getApiKey())
                .build();
        String path = props.getAvailableNetworksPath().startsWith("/")
                ? props.getAvailableNetworksPath()
                : "/" + props.getAvailableNetworksPath();
        try {
            return client.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            return "[]";
        }
    }

    @Override
    public AiInvokeResponse send(NoteappAiProcessRequest request) {
        if (!props.isConfigured()) {
            return AiInvokeResponse.ofFailure(null, "NOT_CONFIGURED");
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
        String path = props.getProcessPath().startsWith("/") ? props.getProcessPath() : "/" + props.getProcessPath();
        try {
            String body = client.post()
                    .uri(path)
                    .body(objectMapper.writeValueAsString(request))
                    .retrieve()
                    .body(String.class);
            return mapAiServiceResponse(body);
        } catch (RestClientResponseException ex) {
            String errBody = ex.getResponseBodyAsString();
            return AiInvokeResponse.ofFailure(errBody, "HTTP_" + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            return AiInvokeResponse.ofFailure(null, "UPSTREAM_ERROR");
        } catch (Exception ex) {
            return AiInvokeResponse.ofFailure(null, "SERIALIZATION_ERROR");
        }
    }

    private AiInvokeResponse mapAiServiceResponse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String status = root.path("status").asText("");
            boolean ok = "success".equalsIgnoreCase(status);
            if (!ok) {
                String err = root.path("errorMessage").asText("");
                String code = err.isEmpty() ? status : err;
                return AiInvokeResponse.ofFailure(body, code);
            }
            AiIntegrationTextExtractor.Parsed p = AiIntegrationTextExtractor.parseSuccessBody(body, objectMapper);
            return AiInvokeResponse.ofSuccess(p.displayText(), p.tokensUsed(), null);
        } catch (Exception e) {
            return AiInvokeResponse.ofSuccess(body == null ? "" : body.trim(), null, null);
        }
    }
}
