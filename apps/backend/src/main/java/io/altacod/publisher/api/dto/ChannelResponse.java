package io.altacod.publisher.api.dto;

import io.altacod.publisher.channel.ChannelType;

import java.time.Instant;

public record ChannelResponse(
        long id,
        ChannelType channelType,
        boolean enabled,
        String label,
        String configMasked,
        Instant updatedAt
) {
}
