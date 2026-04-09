package io.altacod.publisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "publisher.channels.delivery")
public class PublisherChannelDeliveryProperties {

    /**
     * Максимум полных циклов «попытка → ошибка»; после этого {@code retryable=false}.
     */
    private int maxAttempts = 8;

    private int baseDelaySeconds = 45;

    private int maxDelaySeconds = 7200;

    /**
     * ±jitter к задержке (процент от расчётной), чтобы разнести пики.
     */
    private int jitterPercent = 25;

    /**
     * Сколько заданий за один тик планировщика ретраев.
     */
    private int retryBatchSize = 40;

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getBaseDelaySeconds() {
        return baseDelaySeconds;
    }

    public void setBaseDelaySeconds(int baseDelaySeconds) {
        this.baseDelaySeconds = baseDelaySeconds;
    }

    public int getMaxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMaxDelaySeconds(int maxDelaySeconds) {
        this.maxDelaySeconds = maxDelaySeconds;
    }

    public int getJitterPercent() {
        return jitterPercent;
    }

    public void setJitterPercent(int jitterPercent) {
        this.jitterPercent = jitterPercent;
    }

    public int getRetryBatchSize() {
        return retryBatchSize;
    }

    public void setRetryBatchSize(int retryBatchSize) {
        this.retryBatchSize = retryBatchSize;
    }
}
