package io.altacod.publisher.api.dto;

import java.util.List;

public record PublicEngagementDto(
        long likeCount,
        long commentCount,
        boolean likedByMe,
        List<PublicCommentDto> comments
) {
}
