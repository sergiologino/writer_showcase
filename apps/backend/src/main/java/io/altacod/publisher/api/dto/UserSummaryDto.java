package io.altacod.publisher.api.dto;

public record UserSummaryDto(
        long id,
        String email,
        String displayName,
        String locale,
        String timezone,
        String theme
) {
}
