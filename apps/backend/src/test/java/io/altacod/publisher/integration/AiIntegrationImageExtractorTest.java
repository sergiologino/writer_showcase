package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiIntegrationImageExtractorTest {

    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/lS0AAAAASUVORK5CYII=";
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void readsInlineImageFromProviderData() throws Exception {
        var response = json.readTree("{\"data\":[{\"b64_json\":\"" + PNG + "\"}]}");
        assertThat(AiIntegrationImageExtractor.extract(response)).isEqualTo("data:image/png;base64," + PNG);
    }

    @Test
    void downloadsTemporaryHttpsImageUrlBeforeReturningToPublisher() throws Exception {
        var response = json.readTree("{\"data\":[{\"url\":\"https://images.example.com/result.png\"}]}");
        String dataUrl = AiIntegrationImageExtractor.extract(response, uri -> {
            assertThat(uri.getHost()).isEqualTo("images.example.com");
            return Base64.getDecoder().decode(PNG);
        });
        assertThat(dataUrl).isEqualTo("data:image/png;base64," + PNG);
    }

    @Test
    void rejectsLocalOrUnencryptedImageUrl() throws Exception {
        var response = json.readTree("{\"imageUrl\":\"http://127.0.0.1/private.png\"}");
        assertThatThrownBy(() -> AiIntegrationImageExtractor.extract(response, uri ->
                "not an image".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
