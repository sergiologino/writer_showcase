package io.altacod.publisher.channel.publish;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * FIFO: {@code RPUSH} / {@code LPOP}.
 */
public class RedisChannelPublishQueue implements ChannelPublishQueue {

    private final StringRedisTemplate redis;
    private final String key;

    public RedisChannelPublishQueue(StringRedisTemplate redis, String key) {
        this.redis = redis;
        this.key = key;
    }

    @Override
    public void enqueue(long postId) {
        redis.opsForList().rightPush(key, Long.toString(postId));
    }

    @Override
    public Long pollImmediate() {
        String v = redis.opsForList().leftPop(key);
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
