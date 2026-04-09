package io.altacod.publisher.channel.publish;

import java.util.concurrent.ConcurrentLinkedQueue;

public class InMemoryChannelPublishQueue implements ChannelPublishQueue {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void enqueue(long postId) {
        queue.offer(postId);
    }

    @Override
    public Long pollImmediate() {
        return queue.poll();
    }
}
