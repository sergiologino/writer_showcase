package io.altacod.publisher.channel.publish;

/**
 * Очередь заданий на публикацию поста в внешние каналы (in-memory или Redis).
 */
public interface ChannelPublishQueue {

    void enqueue(long postId);

    /**
     * Неблокирующее извлечение; {@code null}, если очередь пуста.
     */
    Long pollImmediate();
}
