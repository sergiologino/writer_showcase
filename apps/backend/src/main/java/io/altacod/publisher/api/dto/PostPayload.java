package io.altacod.publisher.api.dto;

import io.altacod.publisher.channel.ChannelType;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PostPayload(
        @NotBlank @Size(max = 500) String title,
        @Size(max = 500) String slug,
        @Size(max = 20_000) String excerpt,
        @Size(max = 200_000) String bodySource,
        @Size(max = 500_000) String bodyHtml,
        @NotNull PostVisibility visibility,
        @NotNull PostStatus status,
        Long categoryId,
        List<Long> tagIds,
        Boolean aiGenerated,
        List<Long> mediaAssetIds,
        /** null — не менять (update) или по умолчанию true (create). */
        Boolean socialPublishEnabled,
        /**
         * Явный список каналов для кросс-поста. null или пустой список при {@code socialPublishEnabled != false} —
         * все включённые каналы workspace. Непустой — только перечисленные (ВК, ОК и т.д.).
         */
        List<ChannelType> publishChannels
) {
}
