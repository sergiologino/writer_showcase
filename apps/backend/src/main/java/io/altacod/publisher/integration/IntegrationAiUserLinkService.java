package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.config.IntegrationAiProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Однократно при старте: привязка client application (по API-ключу) к пользователю в noteapp-ai-integration
 * через {@code POST /api/admin/clients/{id}/assign-user} (нужен JWT админа).
 */
@Component
public class IntegrationAiUserLinkService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationAiUserLinkService.class);

    private final IntegrationAiProperties props;
    private final ObjectMapper objectMapper;

    public IntegrationAiUserLinkService(IntegrationAiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public void ensureClientLinkedToBuiltinUserIfConfigured() {
        if (!props.isConfigured() || !props.isEnsureClientUserLink()) {
            return;
        }
        if (!StringUtils.hasText(props.getAdminPassword())) {
            return;
        }
        String base = props.getBaseUrl().replaceAll("/$", "");
        RestClient client = buildClient(base);
        String token;
        try {
            token = loginAdmin(client);
        } catch (Exception e) {
            log.warn("integration-ai: admin login failed, skip client-user link: {}", e.getMessage());
            return;
        }
        if (!StringUtils.hasText(token)) {
            return;
        }
        String clientUuid;
        if (StringUtils.hasText(props.getClientId())) {
            try {
                UUID.fromString(props.getClientId().trim());
            } catch (IllegalArgumentException e) {
                log.warn("integration-ai: invalid integration-ai.client-id, skip");
                return;
            }
            clientUuid = props.getClientId().trim();
        } else {
            try {
                clientUuid = findClientIdByApiKey(client, token, props.getApiKey().trim());
            } catch (Exception e) {
                log.warn("integration-ai: could not find client by api key: {}", e.getMessage());
                return;
            }
        }
        if (clientUuid == null) {
            log.warn("integration-ai: no client matches configured api-key; set integration-ai.client-id or fix key");
            return;
        }
        try {
            String assignBody = objectMapper.writeValueAsString(Map.of(
                    "userEmail", props.getAssignBuiltinUserEmail().trim().toLowerCase(),
                    "createUserIfMissing", true,
                    "userFullName", props.getAssignBuiltinUserName() != null ? props.getAssignBuiltinUserName() : "Publisher"
            ));
            client.post()
                    .uri("/api/admin/clients/{id}/assign-user", clientUuid)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(assignBody)
                    .retrieve()
                    .body(String.class);
            log.info("integration-ai: client {} linked to user {} (or already linked)", clientUuid, props.getAssignBuiltinUserEmail());
        } catch (RestClientException e) {
            log.warn("integration-ai: assign-user failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("integration-ai: assign-user error: {}", e.getMessage());
        }
    }

    private String loginAdmin(RestClient client) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", props.getAdminUsername().trim(),
                "password", props.getAdminPassword()
        ));
        String res = client.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        if (res == null) {
            return null;
        }
        JsonNode n = objectMapper.readTree(res);
        return n.path("token").asText(null);
    }

    private String findClientIdByApiKey(RestClient client, String adminBearer, String apiKey) throws Exception {
        String res = client.get()
                .uri("/api/admin/clients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminBearer)
                .retrieve()
                .body(String.class);
        if (res == null) {
            return null;
        }
        JsonNode root = objectMapper.readTree(res);
        if (!root.isArray()) {
            return null;
        }
        for (JsonNode n : root) {
            String k = n.path("apiKey").asText(null);
            if (apiKey.equals(k)) {
                return n.path("id").asText(null);
            }
        }
        return null;
    }

    private static RestClient buildClient(String base) {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(Duration.ofMillis(5_000));
        rf.setReadTimeout(Duration.ofMillis(15_000));
        return RestClient.builder()
                .requestFactory(rf)
                .baseUrl(base)
                .build();
    }
}
