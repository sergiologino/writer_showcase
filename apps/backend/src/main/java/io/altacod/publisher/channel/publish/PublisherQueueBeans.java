package io.altacod.publisher.channel.publish;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PublisherQueueBeans {

    @Bean
    @ConditionalOnProperty(prefix = "publisher.redis", name = "enabled", havingValue = "false", matchIfMissing = true)
    ChannelPublishQueue inMemoryChannelPublishQueue() {
        return new InMemoryChannelPublishQueue();
    }
}
