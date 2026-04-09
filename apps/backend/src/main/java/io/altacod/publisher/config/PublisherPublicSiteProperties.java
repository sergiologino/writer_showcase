package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Базовый URL публичного фронта для ссылок в сообщениях TG/VK (без завершающего /).
 */
@ConfigurationProperties(prefix = "publisher.public-site")
public class PublisherPublicSiteProperties {

    private String baseUrl = "";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean hasBaseUrl() {
        return baseUrl != null && !baseUrl.isBlank();
    }
}
