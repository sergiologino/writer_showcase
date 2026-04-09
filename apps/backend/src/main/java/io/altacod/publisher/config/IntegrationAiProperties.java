package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Параметры noteapp-ai-integration ({@code POST /api/ai/process}, заголовок {@code X-API-Key}).
 * См. {@code docs/ai/EXTERNAL_SERVICES_INTEGRATION.md} в репозитории интеграции.
 */
@ConfigurationProperties(prefix = "publisher.integration-ai")
public class IntegrationAiProperties {

    /**
     * Базовый URL, например {@code http://localhost:8091}.
     */
    private String baseUrl = "";

    /**
     * Ключ клиента {@code aikey_…} для {@code X-API-Key}.
     */
    private String apiKey = "";

    private String apiKeyHeader = "X-API-Key";

    /**
     * Путь обработки (по умолчанию как в noteapp-ai-integration).
     */
    private String processPath = "/api/ai/process";

    private int connectTimeoutMs = 5_000;

    private int readTimeoutMs = 120_000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKeyHeader() {
        return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
        this.apiKeyHeader = apiKeyHeader;
    }

    public String getProcessPath() {
        return processPath;
    }

    public void setProcessPath(String processPath) {
        this.processPath = processPath;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    /**
     * Без {@code api-key} внешний сервис вернёт 401 на {@code /api/ai/**}.
     */
    public boolean isConfigured() {
        return baseUrl != null
                && !baseUrl.isBlank()
                && apiKey != null
                && !apiKey.isBlank();
    }
}
