package io.altacod.publisher.security;

import io.altacod.publisher.config.PublisherProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            PublisherProperties publisherProperties,
            Oauth2ProvidersStatus oauth2ProvidersStatus,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            DelegatingOAuth2UserService delegatingOAuth2UserService
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource(publisherProperties)))
                .sessionManagement(s -> s.sessionCreationPolicy(
                        oauth2ProvidersStatus.isAny()
                                ? SessionCreationPolicy.IF_REQUIRED
                                : SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/robots.txt", "/sitemap.xml").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/engagement/**").authenticated()
                        .anyRequest().authenticated()
                );
        if (oauth2ProvidersStatus.isAny()) {
            http.oauth2Login(
                    o -> o.userInfoEndpoint(u -> u.userService(delegatingOAuth2UserService))
                            .successHandler(oAuth2LoginSuccessHandler));
        }
        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource(PublisherProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        var patterns = properties.cors().allowedOriginPatterns();
        var origins = properties.cors().allowedOrigins();
        if (!origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        } else if (!patterns.isEmpty()) {
            config.setAllowedOriginPatterns(patterns);
        } else {
            config.setAllowedOriginPatterns(List.of(
                    "http://localhost:[*]",
                    "https://localhost:[*]",
                    "http://127.0.0.1:[*]",
                    "https://127.0.0.1:[*]"));
        }
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        // Сессия на cookie не завязана; JWT в Authorization — CORS credentials не обязателен.
        config.setAllowCredentials(false);
        // Chrome: запросы к «более приватному» host (другой порт на loopback) без заголовка → 403 на CORS-этапе
        // (см. DefaultCorsProcessor / Private Network Access).
        config.setAllowPrivateNetwork(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
