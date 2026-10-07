package io.altacod.publisher.channel;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelOutboundLogSentAtTest {

    @Test
    void preservesDeliveryTimeWhenMetricsAreUpdated() {
        Instant created = Instant.parse("2026-10-07T10:00:00Z");
        Instant sent = created.plusSeconds(10);
        ChannelOutboundLogEntity log = new ChannelOutboundLogEntity(
                null, ChannelType.TELEGRAM, ChannelDeliveryStatus.PENDING, null, created);

        log.markSent(sent, "message-id", "https://example.com/post");
        log.setMetricsSnapshot("{}", sent.plusSeconds(90));

        assertEquals(sent, log.getSentAt());
    }
}
