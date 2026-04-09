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
}
