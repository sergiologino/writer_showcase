package io.altacod.publisher.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.api.dto.PageResponse;
import io.altacod.publisher.api.dto.PublicMediaDto;
import io.altacod.publisher.api.dto.PublicPostDetailDto;
import io.altacod.publisher.api.dto.PublicPostSummaryDto;
import io.altacod.publisher.text.PublicArticleHtml;
import io.altacod.publisher.workspace.WorkspaceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Initial HTML for public routes. React takes over the same URL for interactive actions.
 * The private workspace and JSON API continue to use their existing rendering paths.
 */
@RestController
public class PublicHtmlController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu")
            .withLocale(java.util.Locale.forLanguageTag("ru-RU"))
            .withZone(ZoneId.of("Europe/Moscow"));
    private static final String SITE = "Altacod Publisher";
    private static final String LANDING_DESCRIPTION =
            "Altacod Publisher — спокойное рабочее место для авторов: блог, редактор, медиа, AI-помощник и публикация материалов в свои каналы.";

    private final PublicPostService posts;
    private final WorkspaceRepository workspaces;
    private final ObjectMapper objectMapper;
    private final String configuredBaseUrl;

    public PublicHtmlController(
            PublicPostService posts,
            WorkspaceRepository workspaces,
            ObjectMapper objectMapper,
            @Value("${publisher.public-site.base-url:}") String configuredBaseUrl
    ) {
        this.posts = posts;
        this.workspaces = workspaces;
        this.objectMapper = objectMapper;
        this.configuredBaseUrl = configuredBaseUrl == null ? "" : configuredBaseUrl.trim().replaceAll("/+$", "");
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> landing(HttpServletRequest request) throws JsonProcessingException {
        String base = baseUrl(request);
        String canonical = base + "/";
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@context", "https://schema.org");
        schema.put("@type", "WebSite");
        schema.put("name", SITE);
        schema.put("url", base);
        schema.put("inLanguage", "ru");
        StringBuilder body = new StringBuilder();
        body.append("<header class=\"ssr-top\"><div class=\"ssr-wrap ssr-top-inner\">")
                .append(brand()).append("<nav aria-label=\"Навигация сайта\"><a href=\"/#features\">Возможности</a>")
                .append("<a href=\"/#process\">Как это работает</a><a href=\"/login\">Войти</a>")
                .append("<a class=\"ssr-button ssr-button-primary\" href=\"/register\">Начать</a></nav></div></header>");
        body.append("<main><section class=\"ssr-wrap ssr-hero\"><div><p class=\"ssr-kicker\">")
                .append("Для блогеров, писателей и независимых редакций</p>")
                .append("<h1>Ваш текст сначала принадлежит вам.</h1>")
                .append("<p>Altacod Publisher помогает вести авторский блог, хранить материалы как первоисточник и спокойно распространять их в Telegram, соцсети и другие каналы.</p>")
                .append("<div class=\"ssr-actions\"><a class=\"ssr-button ssr-button-primary\" href=\"/register\">Создать авторское пространство →</a>")
                .append("<a class=\"ssr-button\" href=\"/login\">У меня уже есть аккаунт</a></div></div>")
                .append("<img src=\"https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&amp;fit=crop&amp;w=1200&amp;q=78\" ")
                .append("srcset=\"https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&amp;fit=crop&amp;w=640&amp;q=78 640w, ")
                .append("https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&amp;fit=crop&amp;w=960&amp;q=78 960w, ")
                .append("https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&amp;fit=crop&amp;w=1200&amp;q=78 1200w\" ")
                .append("sizes=\"(max-width: 1023px) calc(100vw - 2rem), 520px\" width=\"1200\" height=\"1500\" ")
                .append("alt=\"Рабочий стол автора с ноутбуком и заметками\" fetchpriority=\"high\"></section>");
        body.append("<section id=\"features\" class=\"ssr-feature-band\"><div class=\"ssr-wrap ssr-features\">")
                .append(feature("Пишите в одном месте", "Черновики, медиа, категории и теги живут в вашем рабочем пространстве, а не теряются между сервисами."))
                .append(feature("Публикуйте дальше", "Материал остаётся первоисточником у вас, а сервис помогает отправлять его в каналы и соцсети."))
                .append(feature("Ускоряйте редактуру", "AI-студия и шаблоны промптов помогают подготовить структуру, варианты и адаптацию текста."))
                .append("</div></section>");
        body.append("<section id=\"process\" class=\"ssr-wrap ssr-process\"><img src=\"https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&amp;fit=crop&amp;w=960&amp;q=76\" ")
                .append("width=\"1000\" height=\"800\" loading=\"lazy\" alt=\"Минималистичный рабочий стол с ноутбуком\"><div>")
                .append("<p class=\"ssr-kicker\">Спокойный процесс</p><h2>От заметки до публичного архива</h2>")
                .append("<p>Публикуйте без ощущения, что алгоритмы забирают у вас контроль: собственный блог становится центром, а внешние площадки — каналами доставки.</p></div></section>")
                .append("<ol class=\"ssr-wrap ssr-workflow\"><li>01 Идея</li><li>02 Черновик</li><li>03 Публичный блог</li><li>04 Каналы</li><li>05 Аналитика</li></ol></main>");
        return html(HttpStatus.OK, document(
                "Платформа для блогеров и писателей | " + SITE, LANDING_DESCRIPTION,
                canonical, "website", "https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=1200&q=78",
                false, List.of(schema), body.toString(), null));
    }

    @GetMapping(value = "/blog/{workspaceSlug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> blog(
            @PathVariable String workspaceSlug,
            @RequestParam(required = false) String page,
            HttpServletRequest request
    ) throws JsonProcessingException {
        int pageIndex = pageIndex(page);
        String path = "/blog/" + segment(workspaceSlug);
        String canonical = baseUrl(request) + path + (pageIndex > 0 ? "?page=" + (pageIndex + 1) : "");
        if (!workspaces.existsBySlug(workspaceSlug)) {
            return notFound(canonical, "Блог не найден");
        }
        Page<PublicPostSummaryDto> result = posts.listPublished(workspaceSlug, PageRequest.of(pageIndex, 20));
        boolean noIndex = result.isEmpty();
        String description = "Публичный блог " + workspaceSlug + ": статьи и материалы авторского пространства.";
        String title = "Блог " + workspaceSlug + (pageIndex > 0 ? " — страница " + (pageIndex + 1) : "") + " | " + SITE;
        StringBuilder body = new StringBuilder();
        body.append("<main class=\"ssr-wrap ssr-blog\">").append(publicNav())
                .append("<header class=\"ssr-blog-header\"><p class=\"ssr-kicker\">Авторский блог</p><h1>")
                .append(escape(workspaceSlug)).append("</h1><p>Публичные материалы: ")
                .append(result.getTotalElements()).append("</p></header>");
        if (result.isEmpty()) {
            body.append("<p class=\"ssr-empty\">Публичных материалов пока нет.</p>");
        } else {
            body.append("<ul class=\"ssr-grid\">");
            for (PublicPostSummaryDto post : result.getContent()) {
                String href = path + "/p/" + segment(post.slug());
                body.append("<li><article class=\"ssr-card\">");
                if (post.firstMediaUrl() != null && post.firstMediaMimeType() != null
                        && post.firstMediaMimeType().startsWith("image/")) {
                    body.append("<a href=\"").append(escape(href)).append("\"><img src=\"")
                            .append(escape(post.firstMediaUrl())).append("\" alt=\"\" width=\"640\" height=\"400\" loading=\"lazy\"></a>");
                }
                body.append("<div class=\"ssr-card-content\">");
                if (post.publishedAt() != null) {
                    body.append("<time datetime=\"").append(escape(post.publishedAt().toString())).append("\">")
                            .append(escape(DATE.format(post.publishedAt()))).append("</time>");
                }
                body.append("<h2><a href=\"").append(escape(href)).append("\">")
                        .append(escape(post.title())).append("</a></h2>");
                String summary = StringUtils.hasText(post.excerpt()) ? post.excerpt() : post.bodyPreviewPlain();
                if (StringUtils.hasText(summary)) {
                    body.append("<p>").append(escape(summary.length() > 220 ? summary.substring(0, 220).trim() + "…" : summary)).append("</p>");
                }
                if (StringUtils.hasText(post.authorDisplayName())) {
                    body.append("<span class=\"ssr-author\">").append(escape(post.authorDisplayName())).append("</span>");
                }
                body.append("<a class=\"ssr-read\" href=\"").append(escape(href))
                        .append("\">Читать дальше →</a></div></article></li>");
            }
            body.append("</ul>");
        }
        if (result.getTotalPages() > 1) {
            body.append("<nav class=\"ssr-pagination\" aria-label=\"Страницы блога\">");
            if (pageIndex > 0) {
                body.append("<a href=\"").append(escape(path))
                        .append(pageIndex == 1 ? "" : "?page=" + pageIndex).append("\">← Назад</a>");
            }
            body.append("<span>Страница ").append(pageIndex + 1).append(" из ")
                    .append(result.getTotalPages()).append("</span>");
            if (pageIndex + 1 < result.getTotalPages()) {
                body.append("<a href=\"").append(escape(path)).append("?page=")
                        .append(pageIndex + 2).append("\">Далее →</a>");
            }
            body.append("</nav>");
        }
        body.append("</main>");
        Map<String, Object> schema = Map.of(
                "@context", "https://schema.org", "@type", "Blog",
                "name", workspaceSlug, "url", baseUrl(request) + path);
        return html(HttpStatus.OK, document(title, description, canonical, "website", null,
                noIndex, result.isEmpty() ? List.of() : List.of(schema), body.toString(),
                bootstrap("blog", workspaceSlug, null, pageIndex, PageResponse.from(result))));
    }

    @GetMapping(value = "/blog/{workspaceSlug}/p/{postSlug}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> article(
            @PathVariable String workspaceSlug,
            @PathVariable String postSlug,
            HttpServletRequest request
    ) throws JsonProcessingException {
        String path = "/blog/" + segment(workspaceSlug) + "/p/" + segment(postSlug);
        String canonical = baseUrl(request) + path;
        PublicPostDetailDto post;
        try {
            post = posts.getPublished(workspaceSlug, postSlug);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 404) {
                return notFound(canonical, "Материал не найден");
            }
            throw e;
        }
        post = new PublicPostDetailDto(
                post.id(), post.title(), post.slug(), post.excerpt(), PublicArticleHtml.clean(post.bodyHtml()),
                post.category(), post.tags(), post.media(), post.publishedAt(), post.updatedAt(),
                post.authorId(), post.authorDisplayName(), post.authorAvatarUrl());
        String description = StringUtils.hasText(post.excerpt()) ? post.excerpt()
                : "Статья " + post.title() + " в публичном блоге " + workspaceSlug + ".";
        PublicMediaDto firstImage = post.media().stream()
                .filter(media -> media.mimeType() != null && media.mimeType().startsWith("image/"))
                .findFirst().orElse(null);
        String image = firstImage != null ? baseUrl(request) + firstImage.url()
                : post.authorAvatarUrl() == null ? null : baseUrl(request) + post.authorAvatarUrl();
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@context", "https://schema.org");
        schema.put("@type", "BlogPosting");
        schema.put("headline", post.title());
        schema.put("mainEntityOfPage", canonical);
        if (StringUtils.hasText(post.excerpt())) schema.put("description", post.excerpt());
        if (post.publishedAt() != null) schema.put("datePublished", post.publishedAt());
        if (post.updatedAt() != null) schema.put("dateModified", post.updatedAt());
        if (StringUtils.hasText(post.authorDisplayName())) {
            schema.put("author", Map.of("@type", "Person", "name", post.authorDisplayName()));
        }
        if (firstImage != null) schema.put("image", baseUrl(request) + firstImage.url());
        StringBuilder body = new StringBuilder();
        body.append("<main class=\"ssr-wrap ssr-article\">").append(publicNav())
                .append("<a class=\"ssr-back\" href=\"/blog/").append(escape(segment(workspaceSlug)))
                .append("\">← все материалы</a><article><header class=\"ssr-article-header\">");
        if (post.category() != null) {
            body.append("<span class=\"ssr-category\">").append(escape(post.category().name())).append("</span>");
        }
        body.append("<h1>").append(escape(post.title())).append("</h1>");
        if (StringUtils.hasText(post.authorDisplayName())) {
            body.append("<span class=\"ssr-author\">").append(escape(post.authorDisplayName())).append("</span>");
        }
        if (post.publishedAt() != null) {
            body.append("<time datetime=\"").append(escape(post.publishedAt().toString())).append("\">")
                    .append(escape(DATE.format(post.publishedAt()))).append("</time>");
        }
        body.append("</header>");
        if (StringUtils.hasText(post.excerpt())) {
            body.append("<p class=\"ssr-excerpt\">").append(escape(post.excerpt())).append("</p>");
        }
        for (PublicMediaDto media : post.media()) {
            if (media.mimeType() != null && media.mimeType().startsWith("image/")) {
                body.append("<figure class=\"ssr-media\"><img src=\"").append(escape(media.url()))
                        .append("\" alt=\"").append(escape(media.altText() == null ? "" : media.altText()))
                        .append("\" loading=\"lazy\">");
                if (StringUtils.hasText(media.caption())) {
                    body.append("<figcaption>").append(escape(media.caption())).append("</figcaption>");
                }
                body.append("</figure>");
            } else {
                body.append("<p><a href=\"").append(escape(media.url())).append("\">Скачать вложение #")
                        .append(media.id()).append("</a></p>");
            }
        }
        body.append("<div class=\"article-prose\">")
                .append(post.bodyHtml() == null ? "" : post.bodyHtml())
                .append("</div></article></main>");
        return html(HttpStatus.OK, document(post.title() + " | " + SITE, description, canonical,
                "article", image, false, List.of(schema), body.toString(),
                bootstrap("article", workspaceSlug, postSlug, 0, post)));
    }

    private ResponseEntity<String> notFound(String canonical, String message) throws JsonProcessingException {
        String body = "<main class=\"ssr-wrap ssr-article\">" + publicNav() + "<h1>" + escape(message)
                + "</h1><a href=\"/\">На главную</a></main>";
        return html(HttpStatus.NOT_FOUND, document(message + " | " + SITE, message,
                canonical, "website", null, true, List.of(), body, null));
    }

    private String document(String title, String description, String canonical, String type, String image,
                            boolean noIndex, List<Map<String, Object>> schemas, String body, String bootstrap)
            throws JsonProcessingException {
        String safeTitle = escape(title);
        String safeDescription = escape(description);
        String safeCanonical = escape(canonical);
        String safeImage = escape(image == null ? URI.create(canonical).resolve("/favicon.svg").toString() : image);
        StringBuilder html = new StringBuilder("<!doctype html><html lang=\"ru\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<meta name=\"theme-color\" content=\"#0f1729\">")
                .append("<title>").append(safeTitle).append("</title>")
                .append("<meta name=\"description\" content=\"").append(safeDescription).append("\">")
                .append("<meta name=\"robots\" content=\"")
                .append(noIndex ? "noindex,nofollow" : "index,follow,max-image-preview:large").append("\">")
                .append("<link rel=\"canonical\" href=\"").append(safeCanonical).append("\">")
                .append("<meta property=\"og:site_name\" content=\"").append(SITE).append("\">")
                .append("<meta property=\"og:title\" content=\"").append(safeTitle).append("\">")
                .append("<meta property=\"og:description\" content=\"").append(safeDescription).append("\">")
                .append("<meta property=\"og:type\" content=\"").append(type).append("\">")
                .append("<meta property=\"og:url\" content=\"").append(safeCanonical).append("\">")
                .append("<meta property=\"og:image\" content=\"").append(safeImage).append("\">")
                .append("<meta name=\"twitter:card\" content=\"summary_large_image\">")
                .append("<meta name=\"twitter:title\" content=\"").append(safeTitle).append("\">")
                .append("<meta name=\"twitter:description\" content=\"").append(safeDescription).append("\">")
                .append("<meta name=\"twitter:image\" content=\"").append(safeImage).append("\">")
                .append("<link rel=\"icon\" type=\"image/svg+xml\" href=\"/favicon.svg\">")
                .append("<link rel=\"preconnect\" href=\"https://fonts.googleapis.com\">")
                .append("<link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin>")
                .append("<link href=\"https://fonts.googleapis.com/css2?family=Noto+Serif:wght@400;500;600;700&amp;display=swap\" rel=\"stylesheet\">")
                .append("<link rel=\"stylesheet\" href=\"/assets/app.css\">")
                .append("<script>try{const t=localStorage.getItem('writer-theme');document.documentElement.classList.toggle('dark',t==='dark'||(t!=='light'&&matchMedia('(prefers-color-scheme: dark)').matches))}catch{}</script>");
        for (Map<String, Object> schema : schemas) {
            html.append("<script type=\"application/ld+json\">").append(scriptJson(schema)).append("</script>");
        }
        if (bootstrap != null) html.append("<script>window.__PUBLISHER_BOOTSTRAP__=").append(bootstrap).append("</script>");
        html.append("</head><body><div id=\"root\">").append(body)
                .append("</div><script type=\"module\" src=\"/assets/app.js\"></script></body></html>");
        return html.toString();
    }

    private String bootstrap(String kind, String workspaceSlug, String postSlug, int page, Object data)
            throws JsonProcessingException {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", kind);
        values.put("workspaceSlug", workspaceSlug);
        values.put("postSlug", postSlug);
        values.put("page", page);
        values.put("data", data);
        return scriptJson(values);
    }

    private String scriptJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value)
                .replace("&", "\\u0026").replace("<", "\\u003c").replace(">", "\\u003e")
                .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }

    private ResponseEntity<String> html(HttpStatus status, String content) {
        return ResponseEntity.status(status)
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .cacheControl(CacheControl.noCache())
                .header("X-Content-Type-Options", "nosniff")
                .body(content);
    }

    private String baseUrl(HttpServletRequest request) {
        if (!configuredBaseUrl.isBlank()) return configuredBaseUrl;
        return ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null)
                .replaceQuery(null).build().toUriString();
    }

    private static int pageIndex(String page) {
        if (page == null || page.isBlank()) return 0;
        try {
            int value = Integer.parseInt(page);
            if (value > 0 && value <= 10_000) return value - 1;
        } catch (NumberFormatException ignored) {
            // An invalid page displays the first page, matching the client route.
        }
        return 0;
    }

    private static String segment(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
    }

    private static String feature(String title, String text) {
        return "<article class=\"ssr-card ssr-feature\"><h2>" + escape(title) + "</h2><p>" + escape(text) + "</p></article>";
    }

    private static String brand() {
        return "<a class=\"ssr-brand\" href=\"/\"><img src=\"/favicon.svg\" width=\"32\" height=\"32\" alt=\"\">Publisher</a>";
    }

    private static String publicNav() {
        return "<nav class=\"ssr-public-nav\" aria-label=\"Навигация публичного блога\">" + brand()
                + "<div><a href=\"/login\">Войти</a><a href=\"/register\">Регистрация</a></div></nav>";
    }
}
