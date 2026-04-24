package io.altacod.publisher.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Яндекс API userinfo ожидает заголовок {@code Authorization: OAuth &lt;token&gt;}, не Bearer.
 */
@Service
public class YandexOAuth2UserService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public YandexOAuth2UserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public OAuth2User loadUser(OAuth2UserRequest request) {
        String accessToken = request.getAccessToken().getTokenValue();
        String body;
        try {
            body = restClient.get()
                    .uri("https://login.yandex.ru/info?format=json")
                    .header("Authorization", "OAuth " + accessToken)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_info_error", e.getMessage(), null), e);
        }
        if (body == null || body.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("user_info_error", "Empty body", null));
        }
        try {
            JsonNode n = objectMapper.readTree(body);
            String id = n.path("id").asText(null);
            if (id == null || id.isBlank()) {
                throw new OAuth2AuthenticationException(new OAuth2Error("user_info_error", "No id", null));
            }
            String email = textOrNull(n, "default_email");
            String login = textOrNull(n, "login");
            String displayName = textOrNull(n, "display_name");
            if (displayName == null || displayName.isBlank()) {
                displayName = login != null && !login.isBlank() ? login : id;
            }
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("id", id);
            attrs.put("sub", id);
            attrs.put("email", email);
            attrs.put("name", displayName);
            attrs.put("login", login != null ? login : "");
            return new DefaultOAuth2User(
                    AuthorityUtils.createAuthorityList("ROLE_USER"),
                    attrs,
                    "id"
            );
        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_info_error", e.getMessage(), null), e);
        }
    }

    private static String textOrNull(JsonNode n, String field) {
        String v = n.path(field).asText(null);
        if (v == null || v.isBlank()) {
            return null;
        }
        return v;
    }
}
