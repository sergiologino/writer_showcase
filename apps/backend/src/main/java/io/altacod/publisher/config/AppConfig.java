package io.altacod.publisher.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PublisherProperties.class,
        PublisherStorageProperties.class,
        PublisherRateLimitProperties.class,
        IntegrationAiProperties.class
})
public class AppConfig {
}
