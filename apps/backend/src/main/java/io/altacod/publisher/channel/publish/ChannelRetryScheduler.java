package io.altacod.publisher.channel.publish;

import io.altacod.publisher.channel.ChannelDeliveryStatus;
import io.altacod.publisher.channel.ChannelOutboundLogRepository;
import io.altacod.publisher.channel.ChannelRetryJobRow;
import io.altacod.publisher.config.PublisherChannelDeliveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ChannelRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ChannelRetryScheduler.class);

    private final ChannelOutboundLogRepository outboundLogRepository;
    private final ChannelDispatchService dispatchService;
    private final PublisherChannelDeliveryProperties deliveryProps;

    public ChannelRetryScheduler(
            ChannelOutboundLogRepository outboundLogRepository,
            ChannelDispatchService dispatchService,
            PublisherChannelDeliveryProperties deliveryProps
    ) {
        this.outboundLogRepository = outboundLogRepository;
        this.dispatchService = dispatchService;
        this.deliveryProps = deliveryProps;
    }

    @Scheduled(fixedDelayString = "${publisher.channels.retry-poll-ms:45000}")
    public void retryDue() {
        int batch = Math.max(1, deliveryProps.getRetryBatchSize());
        var page = PageRequest.of(0, batch);
        var due = outboundLogRepository.findDueRetries(ChannelDeliveryStatus.FAILED, Instant.now(), page);
        for (ChannelRetryJobRow row : due) {
            try {
                dispatchService.retryOne(row.getPostId(), row.getChannelType());
            } catch (Exception e) {
                log.warn(
                        "Scheduled channel retry failed postId={} channel={}",
                        row.getPostId(),
                        row.getChannelType(),
                        e
                );
            }
        }
    }
}
