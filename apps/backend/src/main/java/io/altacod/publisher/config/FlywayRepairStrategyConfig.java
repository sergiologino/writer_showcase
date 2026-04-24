package io.altacod.publisher.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairStrategyConfig {

    /**
     * Перед {@code migrate} вызываем {@link org.flywaydb.core.Flyway#repair()}:
     * при изменении текста уже применённой миграции в репозитории выравнивает checksum в
     * {@code flyway_schema_history} (иначе validate падает).
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
