package io.altacod.publisher.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({
        PublisherProperties.class,
        PublisherStorageProperties.class,
        PublisherRateLimitProperties.class,
        IntegrationAiProperties.class,
        PublisherRedisProperties.class,
        PublisherPublicSiteProperties.class,
        PublisherChannelDeliveryProperties.class
})
public class AppConfig {
}
