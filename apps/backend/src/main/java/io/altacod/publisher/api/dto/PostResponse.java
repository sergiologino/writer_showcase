package io.altacod.publisher.api.dto;

import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;

import java.time.Instant;
import java.util.List;

public record PostResponse(
        long id,
        String title,
        String slug,
        String excerpt,
        String bodySource,
        String bodyHtml,
        PostVisibility visibility,
        PostStatus status,
        boolean aiGenerated,
        Long categoryId,
        List<TagSummaryDto> tags,
        List<PostMediaAttachmentDto> media,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant scheduledPublishAt,
        boolean scheduleMissed,
        boolean lateScheduleReleased,
        boolean channelSyndicationBlocked,
        boolean socialPublishEnabled,
        /** Накопительно: сумма токенов по вызовам AI для этой статьи. */
        long aiTokensTotal,
        /** Явно выбранные каналы; пустой список при включённой соцпубликации означает «все каналы workspace». */
        List<ChannelType> publishChannelTypes,
        List<PostOutboundInfoDto> outbound
) {
}
