package io.altacod.publisher.channel.publish;

import io.altacod.publisher.config.PublisherRedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "publisher.redis", name = "enabled", havingValue = "true")
public class PublisherRedisBeans {

    private static final String QUEUE_KEY = "publisher:channel:jobs";

    @Bean
    LettuceConnectionFactory publisherRedisConnectionFactory(PublisherRedisProperties p) {
        RedisStandaloneConfiguration c = new RedisStandaloneConfiguration();
        c.setHostName(p.getHost());
        c.setPort(p.getPort());
        if (p.getPassword() != null && !p.getPassword().isBlank()) {
            c.setPassword(RedisPassword.of(p.getPassword()));
        }
        return new LettuceConnectionFactory(c);
    }

    @Bean
    StringRedisTemplate publisherStringRedisTemplate(LettuceConnectionFactory publisherRedisConnectionFactory) {
        StringRedisTemplate t = new StringRedisTemplate();
        t.setConnectionFactory(publisherRedisConnectionFactory);
        t.afterPropertiesSet();
        return t;
    }

    @Bean
    ChannelPublishQueue redisChannelPublishQueue(StringRedisTemplate publisherStringRedisTemplate) {
        return new RedisChannelPublishQueue(publisherStringRedisTemplate, QUEUE_KEY);
    }
}
