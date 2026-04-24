package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

/**
 * Глобальные настройки безопасности: список email с правами админа (как в других сервисах с {@code ADMIN_EMAILS}).
 */
@ConfigurationProperties(prefix = "publisher.security")
public class PublisherSecurityProperties {

    /**
     * Через запятую, без пробелов вокруг или с пробелами (обрезаются). Регистр не важен.
     */
    private String adminEmails = "";

    public String getAdminEmails() {
        return adminEmails;
    }

    public void setAdminEmails(String adminEmails) {
        this.adminEmails = adminEmails == null ? "" : adminEmails;
    }

    public boolean isListedAdmin(String email) {
        if (email == null || email.isBlank() || adminEmails.isBlank()) {
            return false;
        }
        String e = email.trim().toLowerCase(Locale.ROOT);
        for (String part : adminEmails.split(",")) {
            String t = part.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && t.equals(e)) {
                return true;
            }
        }
        return false;
    }
}
