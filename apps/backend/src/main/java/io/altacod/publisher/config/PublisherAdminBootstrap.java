package io.altacod.publisher.config;

import io.altacod.publisher.user.UserEntity;
import io.altacod.publisher.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Опционально: пользователь-админ для входа (логин/пароль из настроек).
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "publisher.admin-bootstrap.enabled", havingValue = "true")
public class PublisherAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PublisherAdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PublisherAdminBootstrapProperties props;

    public PublisherAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PublisherAdminBootstrapProperties props
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.getEmail().trim().toLowerCase();
        var existing = userRepository.findByEmailIgnoreCase(email);
        Instant now = Instant.now();
        if (existing.isEmpty()) {
            var u = new UserEntity(
                    email,
                    passwordEncoder.encode(props.getPassword()),
                    props.getDisplayName(),
                    "ru",
                    "Europe/Moscow",
                    "system",
                    now
            );
            u.setAdmin(true);
            userRepository.save(u);
            log.info("Bootstrap: created admin user {}", email);
            return;
        }
        UserEntity u = existing.get();
        if (!u.isAdmin()) {
            u.setAdmin(true);
            u.touch(now);
            log.info("Bootstrap: granted admin to {}", email);
        }
    }
}
