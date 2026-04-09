package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "publisher.rate-limit")
public class PublisherRateLimitProperties {

    private boolean enabled = true;
    /** Лимит на IP для /api/auth/login, /register, /refresh (запросов в минуту). */
    private int authPerMinute = 40;
    /** Лимит на IP для остального /api/* (запросов в минуту). */
    private int apiPerMinute = 400;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getAuthPerMinute() {
        return authPerMinute;
    }

    public void setAuthPerMinute(int authPerMinute) {
        this.authPerMinute = authPerMinute;
    }

    public int getApiPerMinute() {
        return apiPerMinute;
    }

    public void setApiPerMinute(int apiPerMinute) {
        this.apiPerMinute = apiPerMinute;
    }
}
