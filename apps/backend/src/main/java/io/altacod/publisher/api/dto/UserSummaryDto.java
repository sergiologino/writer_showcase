package io.altacod.publisher.api.dto;

import java.util.List;

public record UserSummaryDto(
        long id,
        String email,
        String displayName,
        String locale,
        String timezone,
        String theme,
        boolean isAdmin,
        /** null — нет аватара */
        String avatarUrl,
        /** Фотографии профиля (галерея); публичные URL. */
        List<ProfilePhotoDto> profilePhotos
) {
}
