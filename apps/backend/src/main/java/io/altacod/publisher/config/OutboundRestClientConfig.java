package io.altacod.publisher.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Единый {@link RestClient} для исходящих вызовов к внешним API каналов.
 * Свойства JVM (системный прокси и т.д.) применяются до {@link RestClient#create()}.
 */
@Configuration
@EnableConfigurationProperties(PublisherOutboundHttpProperties.class)
public class OutboundRestClientConfig {

    public static final String OUTBOUND_REST_CLIENT = "outboundRestClient";

    @Bean(name = OUTBOUND_REST_CLIENT)
    public RestClient outboundRestClient(PublisherOutboundHttpProperties httpProps) {
        httpProps.applyJvmNetworkingProperties();
        return RestClient.create();
    }
}
