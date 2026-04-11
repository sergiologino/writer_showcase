package io.altacod.publisher.channel;

/**
 * Убирает из текстов ошибок секреты (токены в URL и query), чтобы не попадали в БД и в UI.
 */
public final class ChannelErrorSanitizer {

    private ChannelErrorSanitizer() {
    }

    public static String stripSecrets(String s) {
        if (s == null || s.isBlank()) {
            return s;
        }
        String out = s;
        // https://api.telegram.org/bot<token>/method
        out = out.replaceAll(
                "(?i)https://api\\.telegram\\.org/bot[0-9]+:[A-Za-z0-9_-]+",
                "https://api.telegram.org/bot***"
        );
        // VK / OAuth-style query params in exception text
        out = out.replaceAll("(?i)(access_token=)[^&\\s\"']+", "$1***");
        out = out.replaceAll("(?i)(client_secret=)[^&\\s\"']+", "$1***");
        return out;
    }

    /**
     * Одна строка для WARN-логов без утечки секретов из вложенных причин.
     */
    public static String sanitizeThrowableChainForLog(Throwable t) {
        if (t == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable cur = t;
        int depth = 0;
        while (cur != null && depth < 10) {
            String part = cur.getClass().getSimpleName();
            if (cur.getMessage() != null && !cur.getMessage().isBlank()) {
                part += ": " + stripSecrets(cur.getMessage());
            }
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(part);
            cur = cur.getCause();
            depth++;
        }
        String s = sb.toString();
        if (s.length() > 900) {
            return s.substring(0, 897) + "…";
        }
        return s;
    }
}
