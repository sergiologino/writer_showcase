package io.altacod.publisher.api;

import io.altacod.publisher.post.PostEntity;
import io.altacod.publisher.post.PostRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.Comparator;

@RestController
public class SeoController {

    private final PostRepository postRepository;
    private final String configuredBaseUrl;

    public SeoController(PostRepository postRepository, @Value("${publisher.public-site.base-url:}") String configuredBaseUrl) {
        this.postRepository = postRepository;
        this.configuredBaseUrl = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
    }

    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public String robots(HttpServletRequest request) {
        String baseUrl = baseUrl(request);
        return """
                User-agent: *
                Allow: /
                Disallow: /app/
                Disallow: /auth/
                Disallow: /login
                Disallow: /register

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap(HttpServletRequest request) {
        String baseUrl = baseUrl(request);
        StringBuilder xml = new StringBuilder();
        xml.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        appendUrl(xml, baseUrl + "/", Instant.now(), "daily", "1.0");
        postRepository.findPublicWorkspaceSlugs().forEach(slug ->
                appendUrl(xml, baseUrl + "/blog/" + escapeUrl(slug), Instant.now(), "daily", "0.8"));
        postRepository.findPublishedPublicForSitemap().stream()
                .sorted(Comparator.comparing(this::lastModified).reversed())
                .forEach(post -> appendUrl(
                        xml,
                        baseUrl + "/blog/" + escapeUrl(post.getWorkspace().getSlug()) + "/p/" + escapeUrl(post.getSlug()),
                        lastModified(post),
                        "weekly",
                        "0.9"
                ));
        xml.append("</urlset>\n");
        return xml.toString();
    }

    private String baseUrl(HttpServletRequest request) {
        if (!configuredBaseUrl.isBlank()) {
            return stripTrailingSlash(configuredBaseUrl);
        }
        return stripTrailingSlash(ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath(null)
                .replaceQuery(null)
                .build()
                .toUriString());
    }

    private static String stripTrailingSlash(String value) {
        String out = value;
        while (out.endsWith("/") && out.length() > "https://x".length()) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private Instant lastModified(PostEntity post) {
        if (post.getPublishedAt() != null) {
            return post.getPublishedAt();
        }
        return post.getUpdatedAt() != null ? post.getUpdatedAt() : post.getCreatedAt();
    }

    private static void appendUrl(StringBuilder xml, String loc, Instant lastmod, String changefreq, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        xml.append("    <lastmod>").append(lastmod.toString()).append("</lastmod>\n");
        xml.append("    <changefreq>").append(changefreq).append("</changefreq>\n");
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    private static String escapeUrl(String value) {
        return value.replace(" ", "%20");
    }

    private static String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
