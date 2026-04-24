package io.altacod.publisher.integration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Выполняет привязку клиента к пользователю в noteapp-ai-integration после старта контекста.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class IntegrationAiUserLinkRunner implements ApplicationRunner {

    private final IntegrationAiUserLinkService linkService;

    public IntegrationAiUserLinkRunner(IntegrationAiUserLinkService linkService) {
        this.linkService = linkService;
    }

    @Override
    public void run(ApplicationArguments args) {
        linkService.ensureClientLinkedToBuiltinUserIfConfigured();
    }
}
