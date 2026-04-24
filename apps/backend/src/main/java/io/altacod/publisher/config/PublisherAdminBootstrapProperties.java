package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "publisher.admin-bootstrap")
public class PublisherAdminBootstrapProperties {

    /**
     * Создать/пометить администратора при старте (только dev/первичная настройка).
     */
    private boolean enabled = false;

    private String email = "admin@local.test";

    private String password = "admin";

    private String displayName = "Admin";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
