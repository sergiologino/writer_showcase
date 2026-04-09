package io.altacod.publisher.channel.publish;

/**
 * Пост впервые перешёл в состояние {@code PUBLISHED} + {@code PUBLIC} (после коммита транзакции).
 */
public record PostPublishedEvent(long postId) {
}
