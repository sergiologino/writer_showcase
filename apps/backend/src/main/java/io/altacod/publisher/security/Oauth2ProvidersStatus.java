package io.altacod.publisher.security;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Какие OAuth2-провайдеры настроены (и для Security, и для /api/auth/oauth2/providers).
 */
public final class Oauth2ProvidersStatus {

    private final boolean google;
    private final boolean yandex;

    public Oauth2ProvidersStatus(
            String googleId,
            String googleSecret,
            String yandexId,
            String yandexSecret
    ) {
        this.google = StringUtils.hasText(googleId) && StringUtils.hasText(googleSecret);
        this.yandex = StringUtils.hasText(yandexId) && StringUtils.hasText(yandexSecret);
    }

    public boolean isGoogle() {
        return google;
    }

    public boolean isYandex() {
        return yandex;
    }

    public boolean isAny() {
        return google || yandex;
    }

    public List<String> enabledRegistrationIds() {
        if (google && yandex) {
            return List.of("google", "yandex");
        }
        if (google) {
            return List.of("google");
        }
        if (yandex) {
            return List.of("yandex");
        }
        return List.of();
    }
}
