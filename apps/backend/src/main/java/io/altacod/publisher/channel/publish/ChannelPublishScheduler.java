package io.altacod.publisher.channel.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ChannelPublishScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChannelPublishScheduler.class);

    private static final int BATCH = 32;

    private final ChannelPublishQueue queue;
    private final ChannelDispatchService dispatchService;

    public ChannelPublishScheduler(ChannelPublishQueue queue, ChannelDispatchService dispatchService) {
        this.queue = queue;
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelayString = "${publisher.channels.poll-delay-ms:2000}")
    public void drain() {
        for (int i = 0; i < BATCH; i++) {
            Long postId = queue.pollImmediate();
            if (postId == null) {
                break;
            }
            try {
                dispatchService.process(postId);
            } catch (Exception e) {
                log.warn("Channel publish job failed for post {}", postId, e);
            }
        }
    }
}
