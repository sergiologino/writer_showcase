package io.altacod.publisher.api.dto;

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
        Boolean aiGenerated
) {
}
