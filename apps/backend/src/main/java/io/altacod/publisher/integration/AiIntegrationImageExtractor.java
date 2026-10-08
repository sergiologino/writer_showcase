package io.altacod.publisher.integration;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/** Converts a trusted AI image result into a durable data URL before its temporary provider URL expires. */
final class AiIntegrationImageExtractor {

    private static final int MAX_IMAGE_BYTES = 20 * 1024 * 1024;

    private AiIntegrationImageExtractor() {
    }

    @FunctionalInterface
    interface ImageDownloader {
        byte[] download(URI uri) throws Exception;
    }

    static String extract(JsonNode response) throws Exception {
        return extract(response, AiIntegrationImageExtractor::download);
    }

    static String extract(JsonNode response, ImageDownloader downloader) throws Exception {
        if (response == null || response.isNull()) return null;
        JsonNode item = response;
        if (response.path("data").isArray() && !response.path("data").isEmpty()) {
            item = response.path("data").get(0);
        } else if (response.path("output").isArray() && !response.path("output").isEmpty()) {
            item = response.path("output").get(0);
        }
        String encoded = firstText(item, "b64_json", "base64", "imageBase64");
        if (encoded == null) encoded = firstText(response, "imageBase64");
        if (encoded != null) {
            int comma = encoded.indexOf(',');
            if (encoded.startsWith("data:") && comma > 0) encoded = encoded.substring(comma + 1);
            return asDataUrl(Base64.getDecoder().decode(encoded));
        }
        String url = firstText(item, "url", "imageUrl");
        if (url == null) url = firstText(response, "imageUrl", "sourceImageUrl");
        if (url == null) return null;
        URI uri = URI.create(url);
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                || host.equalsIgnoreCase("localhost") || host.endsWith(".local")
                || host.matches("[0-9.]+") || host.contains(":")) {
            throw new IllegalArgumentException("AI image URL must use a public HTTPS host");
        }
        return asDataUrl(downloader.download(uri));
    }

    private static String firstText(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) return null;
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && value.isTextual() && !value.asText().isBlank()) return value.asText();
        }
        return null;
    }

    private static String asDataUrl(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("AI image is empty or too large");
        }
        String mime;
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            mime = "image/png";
        } else if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff) {
            mime = "image/jpeg";
        } else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            mime = "image/webp";
        } else if (bytes.length >= 4 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') {
            mime = "image/gif";
        } else {
            throw new IllegalArgumentException("AI response is not a supported image");
        }
        return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    private static byte[] download(URI uri) throws Exception {
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("AI image URL resolves to a private address");
            }
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                throw new IllegalStateException("AI image download returned HTTP " + response.statusCode());
            }
            byte[] bytes = body.readNBytes(MAX_IMAGE_BYTES + 1);
            if (bytes.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("AI image is too large");
            return bytes;
        }
    }
}
