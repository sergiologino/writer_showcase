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

    public record CorsProperties(List<String> allowedOrigins) {
    }
}
