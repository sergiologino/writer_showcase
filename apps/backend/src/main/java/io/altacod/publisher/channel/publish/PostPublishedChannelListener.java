package io.altacod.publisher.channel.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PostPublishedChannelListener {

    private static final Logger log = LoggerFactory.getLogger(PostPublishedChannelListener.class);

    private final ChannelPublishQueue queue;

    public PostPublishedChannelListener(ChannelPublishQueue queue) {
        this.queue = queue;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPublished(PostPublishedEvent event) {
        try {
            queue.enqueue(event.postId());
        } catch (Exception e) {
            log.warn("Failed to enqueue channel publish job for post {}", event.postId(), e);
        }
    }
}
