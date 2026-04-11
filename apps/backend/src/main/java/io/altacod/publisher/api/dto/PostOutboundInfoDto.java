package io.altacod.publisher.api.dto;

import io.altacod.publisher.channel.ChannelDeliveryStatus;
import io.altacod.publisher.channel.ChannelType;

import java.time.Instant;

public record PostOutboundInfoDto(
        ChannelType channelType,
        ChannelDeliveryStatus deliveryStatus,
        String externalUrl,
        String lastError,
        Instant metricsFetchedAt,
        Long likes,
        Long reposts,
        Long views,
        Long comments,
        Long shares
) {
    public static PostOutboundInfoDto pending(ChannelType channelType) {
        return new PostOutboundInfoDto(
                channelType,
                ChannelDeliveryStatus.PENDING,
                null,
                null,
                null,
                0L,
                0L,
                0L,
                0L,
                0L
        );
    }
}
