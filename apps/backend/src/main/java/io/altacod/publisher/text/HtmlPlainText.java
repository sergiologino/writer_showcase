package io.altacod.publisher.text;

/**
 * Грубое извлечение текста из HTML без полноценного парсера (превью для ленты и каналов).
 */
public final class HtmlPlainText {

    private HtmlPlainText() {
    }

    public static String toPlain(String html, int maxChars) {
        if (html == null || html.isBlank()) {
            return null;
        }
        String t = html
                .replaceAll("(?i)<\\s*br\\s*/?>", "\n")
                .replaceAll("(?i)</\\s*p\\s*>", "\n\n")
                .replaceAll("(?i)</\\s*(div|section|article|blockquote|h[1-6]|li|tr|table)\\s*>", "\n")
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .replaceAll(" +", " ")
                .trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() <= maxChars) {
            return t;
        }
        return t.substring(0, maxChars - 1) + "…";
    }

    public static String truncatePlain(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars - 1) + "…";
    }
}
