package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "publisher")
public record PublisherProperties(JwtProperties jwt, CorsProperties cors) {

    public record JwtProperties(String secret, int accessTtlMinutes, int refreshTtlDays) {
        public JwtProperties {
            if (refreshTtlDays < 1) {
                refreshTtlDays = 30;
            }
        }
    }

    /**
     * @param allowedOrigins        точные origin (например продакшен HTTPS)
     * @param allowedOriginPatterns паттерны Spring CORS (например {@code http://localhost:*} — любой порт)
     */
    public record CorsProperties(List<String> allowedOrigins, List<String> allowedOriginPatterns) {

        public CorsProperties {
            if (allowedOrigins == null) {
                allowedOrigins = List.of();
            }
            if (allowedOriginPatterns == null) {
                allowedOriginPatterns = List.of();
            }
        }
    }
}
