package io.altacod.publisher.text;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/** Safe public representation of author HTML; the stored source is not changed. */
public final class PublicArticleHtml {

    private static final Safelist ARTICLE = Safelist.relaxed()
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "figure", "figcaption", "video", "source", "iframe")
            .addAttributes("img", "alt", "width", "height", "loading")
            .addAttributes("video", "src", "controls", "poster", "width", "height", "preload")
            .addAttributes("source", "src", "type")
            .addAttributes("iframe", "src", "title", "width", "height", "loading", "allow", "allowfullscreen")
            .addProtocols("img", "src", "http", "https")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("video", "poster", "http", "https")
            .addProtocols("source", "src", "http", "https")
            .addProtocols("iframe", "src", "https")
            .addEnforcedAttribute("iframe", "sandbox", "allow-scripts allow-popups allow-presentation")
            .preserveRelativeLinks(true);

    private PublicArticleHtml() {
    }

    public static String clean(String html) {
        if (html == null || html.isBlank()) return "";
        return Jsoup.clean(html, "https://publisher.invalid", ARTICLE,
                new Document.OutputSettings().prettyPrint(false));
    }
}
