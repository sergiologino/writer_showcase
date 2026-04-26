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

    private String availableNetworksPath = "/api/ai/networks/available";

    /**
     * Публикация в соцсети (Telegram, Facebook, X) через тот же сервис и тот же {@code X-API-Key}.
     */
    private String socialPostsPath = "/api/social/posts";

    private int connectTimeoutMs = 5_000;

    private int readTimeoutMs = 120_000;

    /**
     * При старте: войти в noteapp-ai-integration как админ и привязать текущий API-клиент
     * к пользователю {@link #assignBuiltinUserEmail} (создать user_accounts при необходимости).
     * Снимает ошибки вида «клиент не привязан к пользователю» в оркестраторе.
     */
    private boolean ensureClientUserLink = true;

    private String adminUsername = "admin";

    /**
     * Пароль админа в integration (MVP: admin). Пусто — автопривязка пропускается.
     */
    private String adminPassword = "";

    private String assignBuiltinUserEmail = "publisher-builtin@integration.local";

    private String assignBuiltinUserName = "Publisher (auto)";

    /**
     * Необязательно: UUID клиента в integration; если задан, поиск по api-key в списке не выполняется.
     */
    private String clientId;

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

    public String getAvailableNetworksPath() {
        return availableNetworksPath;
    }

    public void setAvailableNetworksPath(String availableNetworksPath) {
        this.availableNetworksPath = availableNetworksPath;
    }

    public String getSocialPostsPath() {
        return socialPostsPath;
    }

    public void setSocialPostsPath(String socialPostsPath) {
        this.socialPostsPath = socialPostsPath;
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

    public boolean isEnsureClientUserLink() {
        return ensureClientUserLink;
    }

    public void setEnsureClientUserLink(boolean ensureClientUserLink) {
        this.ensureClientUserLink = ensureClientUserLink;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAssignBuiltinUserEmail() {
        return assignBuiltinUserEmail;
    }

    public void setAssignBuiltinUserEmail(String assignBuiltinUserEmail) {
        this.assignBuiltinUserEmail = assignBuiltinUserEmail;
    }

    public String getAssignBuiltinUserName() {
        return assignBuiltinUserName;
    }

    public void setAssignBuiltinUserName(String assignBuiltinUserName) {
        this.assignBuiltinUserName = assignBuiltinUserName;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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
