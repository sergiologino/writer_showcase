package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.api.dto.AiInvokeResponse;
import io.altacod.publisher.config.IntegrationAiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * HTTP-клиент к noteapp-ai-integration: {@code POST /api/ai/process} с {@code X-API-Key} (без Bearer).
 * Документация: {@code noteapp-ai-integration/docs/ai/EXTERNAL_SERVICES_INTEGRATION.md}.
 */
@Component
public class HttpIntegrationAiClient implements IntegrationAiClient {

    private static final Logger log = LoggerFactory.getLogger(HttpIntegrationAiClient.class);

    private final IntegrationAiProperties props;
    private final ObjectMapper objectMapper;

    public HttpIntegrationAiClient(IntegrationAiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Override
    public String fetchAvailableNetworksJson() {
        if (!props.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Сервис нейросетей не настроен в Publisher");
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
            String body = client.get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("Available networks endpoint returned an empty response");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Сервис нейросетей вернул пустой ответ");
            }
            return body;
        } catch (RestClientResponseException e) {
            log.warn("Available networks endpoint returned HTTP {}", e.getStatusCode().value());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Сервис нейросетей ответил HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            log.warn("Available networks request failed: {}", e.toString());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Не удалось связаться с сервисом нейросетей", e);
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
            return mapAiServiceResponse(body, request.requestType());
        } catch (RestClientResponseException ex) {
            String errBody = ex.getResponseBodyAsString();
            return AiInvokeResponse.ofFailure(errBody, "HTTP_" + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            return AiInvokeResponse.ofFailure(null, "UPSTREAM_ERROR");
        } catch (Exception ex) {
            return AiInvokeResponse.ofFailure(null, "SERIALIZATION_ERROR");
        }
    }

    private AiInvokeResponse mapAiServiceResponse(String body, String requestType) {
        try {
            JsonNode root = objectMapper.readTree(body);
            String status = root.path("status").asText("");
            boolean ok = "success".equalsIgnoreCase(status);
            if (!ok) {
                String err = root.path("errorMessage").asText("");
                String code = err.isEmpty() ? status : err;
                return AiInvokeResponse.ofFailure(body, code);
            }
            if ("image_generation".equalsIgnoreCase(requestType) || "image_edit".equalsIgnoreCase(requestType)) {
                JsonNode imageResponse = root.path("response");
                if (imageResponse.isTextual()) imageResponse = objectMapper.readTree(imageResponse.asText());
                String image = AiIntegrationImageExtractor.extract(imageResponse);
                if (image == null) return AiInvokeResponse.ofFailure(null, "IMAGE_NOT_RETURNED");
                Integer tokens = root.path("tokensUsed").isNumber() ? root.path("tokensUsed").asInt() : null;
                return AiInvokeResponse.ofSuccess("Изображение готово", tokens, null, image);
            }
            AiIntegrationTextExtractor.Parsed p = AiIntegrationTextExtractor.parseSuccessBody(body, objectMapper);
            return AiInvokeResponse.ofSuccess(p.displayText(), p.tokensUsed(), null);
        } catch (Exception e) {
            log.warn("Could not read AI response: {}", e.toString());
            if ("image_generation".equalsIgnoreCase(requestType) || "image_edit".equalsIgnoreCase(requestType)) {
                return AiInvokeResponse.ofFailure(null, "INVALID_AI_IMAGE");
            }
            return AiInvokeResponse.ofSuccess(body == null ? "" : body.trim(), null, null);
        }
    }
}
