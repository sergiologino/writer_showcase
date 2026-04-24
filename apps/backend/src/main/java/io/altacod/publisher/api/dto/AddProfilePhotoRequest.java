package io.altacod.publisher.api.dto;

import jakarta.validation.constraints.NotNull;

public record AddProfilePhotoRequest(
        @NotNull
        Long mediaAssetId
) {
}
