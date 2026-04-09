package io.altacod.publisher.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Подхватывает переменные из {@code .env} и {@code .env.local} в каталоге {@code user.dir}
 * (для IDEA: рабочая директория модуля {@code apps/backend}). Файлы не коммитятся.
 * {@code .env.local} переопределяет ключи из {@code .env}.
 * В профиле {@code test} отключено, чтобы интеграционные тесты не подтягивали локальную БД.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.acceptsProfiles(Profiles.of("test"))) {
            return;
        }
        Path dir = Paths.get(System.getProperty("user.dir"));
        Map<String, Object> combined = new LinkedHashMap<>();
        loadInto(dir, ".env", combined);
        loadInto(dir, ".env.local", combined);
        if (combined.isEmpty()) {
            return;
        }
        MutablePropertySources sources = environment.getPropertySources();
        sources.addFirst(new MapPropertySource("dotenv-files", combined));
    }

    private static void loadInto(Path dir, String filename, Map<String, Object> target) {
        Path file = dir.resolve(filename);
        if (!Files.isRegularFile(file)) {
            return;
        }
        Dotenv dotenv = Dotenv.configure()
                .directory(dir.toString())
                .filename(filename)
                .ignoreIfMissing()
                .load();
        for (DotenvEntry e : dotenv.entries()) {
            target.put(e.getKey(), e.getValue());
        }
    }
}
