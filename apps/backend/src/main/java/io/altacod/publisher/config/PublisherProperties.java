package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "publisher")
public record PublisherProperties(JwtProperties jwt, CorsProperties cors) {

    public record JwtProperties(String secret, int accessTtlMinutes) {
    }

    public record CorsProperties(List<String> allowedOrigins) {
    }
}
