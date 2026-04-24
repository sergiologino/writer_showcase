package io.altacod.publisher.security;

import io.altacod.publisher.api.AuthService;
import io.altacod.publisher.api.dto.TokenResponse;
import io.altacod.publisher.config.PublisherOAuth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final PublisherOAuth2Properties oauth2Properties;

    public OAuth2LoginSuccessHandler(
            AuthService authService,
            PublisherOAuth2Properties oauth2Properties
    ) {
        this.authService = authService;
        this.oauth2Properties = oauth2Properties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unexpected authentication type");
        }
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2User principal = (OAuth2User) token.getPrincipal();
        String email;
        String displayName;
        String subject;
        if (principal instanceof OidcUser oidc) {
            email = oidc.getEmail();
            if (email == null || email.isBlank()) {
                Object a = oidc.getAttribute("email");
                email = a != null ? String.valueOf(a) : null;
            }
            String full = oidc.getFullName();
            if (full == null || full.isBlank()) {
                String g = oidc.getGivenName();
                String f = oidc.getFamilyName();
                if (g != null && f != null) {
                    full = (g + " " + f).trim();
                } else {
                    full = g != null ? g : f;
                }
            }
            displayName = (full != null && !full.isBlank()) ? full : oidc.getPreferredUsername();
            subject = oidc.getSubject();
        } else {
            Object emailAttr = principal.getAttribute("email");
            email = emailAttr != null ? String.valueOf(emailAttr) : null;
            Object nameAttr = principal.getAttribute("name");
            displayName = nameAttr != null ? String.valueOf(nameAttr) : null;
            subject = principal.getName();
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OAuth: email is missing");
        }
        try {
            TokenResponse tr = authService.signInWithOAuth(
                    registrationId,
                    subject,
                    email,
                    displayName
            );
            String base = stripTrailingSlash(oauth2Properties.getFrontendBaseUrl());
            String hash = "access_token=" + enc(tr.accessToken())
                    + "&refresh_token=" + enc(tr.refreshToken())
                    + "&token_type=" + enc(tr.tokenType())
                    + "&expires_in=" + tr.expiresIn();
            getRedirectStrategy().sendRedirect(request, response, base + "/auth/callback#" + hash);
        } catch (ResponseStatusException e) {
            String base = stripTrailingSlash(oauth2Properties.getFrontendBaseUrl());
            String param = e.getStatusCode() == HttpStatus.CONFLICT
                    ? "link_conflict" : "error";
            getRedirectStrategy().sendRedirect(
                    request,
                    response,
                    base + "/login?oauth=" + enc(param)
            );
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:5173";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
