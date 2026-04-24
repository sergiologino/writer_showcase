package io.altacod.publisher.api.dto;

import org.springframework.lang.Nullable;

/** {@code null} mediaAssetId — сбросить аватар. */
public record SetAvatarRequest(@Nullable Long mediaAssetId) {
}
