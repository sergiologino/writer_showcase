package io.altacod.publisher.security;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class DelegatingOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final DefaultOAuth2UserService defaultService = new DefaultOAuth2UserService();
    private final YandexOAuth2UserService yandexOAuth2UserService;

    public DelegatingOAuth2UserService(YandexOAuth2UserService yandexOAuth2UserService) {
        this.yandexOAuth2UserService = yandexOAuth2UserService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        if ("yandex".equals(userRequest.getClientRegistration().getRegistrationId())) {
            return yandexOAuth2UserService.loadUser(userRequest);
        }
        return defaultService.loadUser(userRequest);
    }
}
