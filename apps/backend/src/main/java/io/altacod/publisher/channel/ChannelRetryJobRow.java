package io.altacod.publisher.channel;

/**
 * Срез строки лога для планировщика ретраев.
 */
public interface ChannelRetryJobRow {

    Long getPostId();

    ChannelType getChannelType();
}
