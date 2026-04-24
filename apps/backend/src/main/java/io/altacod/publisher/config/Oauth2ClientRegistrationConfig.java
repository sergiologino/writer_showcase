package io.altacod.publisher.config;

import io.altacod.publisher.security.Oauth2ProvidersStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class Oauth2ClientRegistrationConfig {

    @Bean
    public Oauth2ProvidersStatus oauth2ProvidersStatus(
            @Value("${GOOGLE_CLIENT_ID:}") String googleId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleSecret,
            @Value("${YANDEX_CLIENT_ID:}") String yandexId,
            @Value("${YANDEX_CLIENT_SECRET:}") String yandexSecret
    ) {
        return new Oauth2ProvidersStatus(googleId, googleSecret, yandexId, yandexSecret);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            Oauth2ProvidersStatus status,
            @Value("${GOOGLE_CLIENT_ID:}") String googleId,
            @Value("${GOOGLE_CLIENT_SECRET:}") String googleSecret,
            @Value("${YANDEX_CLIENT_ID:}") String yandexId,
            @Value("${YANDEX_CLIENT_SECRET:}") String yandexSecret
    ) {
        if (!status.isAny()) {
            return new InMemoryClientRegistrationRepository();
        }
        List<ClientRegistration> list = new ArrayList<>();
        if (status.isGoogle()) {
            list.add(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                    .clientId(googleId.trim())
                    .clientSecret(googleSecret.trim())
                    .build());
        }
        if (status.isYandex()) {
            list.add(yandexClient(yandexId.trim(), yandexSecret.trim()));
        }
        return new InMemoryClientRegistrationRepository(list);
    }

    private static ClientRegistration yandexClient(String id, String secret) {
        return ClientRegistration.withRegistrationId("yandex")
                .clientId(id)
                .clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("login:info", "login:email")
                .authorizationUri("https://oauth.yandex.ru/authorize")
                .tokenUri("https://oauth.yandex.ru/token")
                .userInfoUri("https://login.yandex.ru/info?format=json")
                .userNameAttributeName("id")
                .clientName("Yandex")
                .build();
    }
}
