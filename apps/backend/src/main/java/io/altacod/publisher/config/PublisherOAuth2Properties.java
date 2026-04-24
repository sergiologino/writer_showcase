package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URL фронтенд-приложения (без / на конце), на который бэкенд редиректит после
 * успешного OAuth2 с фрагментом access_token / refresh_token.
 */
@ConfigurationProperties(prefix = "publisher.oauth2")
public class PublisherOAuth2Properties {

    private String frontendBaseUrl = "http://localhost:5173";

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }
}
