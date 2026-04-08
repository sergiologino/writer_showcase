package io.altacod.publisher.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugify {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s_]+");
    private static final Pattern MULTI_HYPHEN = Pattern.compile("-+");

    private Slugify() {
    }

    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "item";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String lower = normalized.toLowerCase(Locale.ROOT);
        String spaced = WHITESPACE.matcher(lower).replaceAll("-");
        String cleaned = NON_LATIN.matcher(spaced).replaceAll("");
        String collapsed = MULTI_HYPHEN.matcher(cleaned).replaceAll("-");
        String trimmed = collapsed.replaceAll("^-+", "").replaceAll("-+$", "");
        return trimmed.isEmpty() ? "item" : trimmed;
    }

    public static String uniqueSlug(String base, java.util.function.Predicate<String> exists) {
        String candidate = slugify(base);
        if (!exists.test(candidate)) {
            return candidate;
        }
        for (int i = 2; i < 10_000; i++) {
            String withSuffix = candidate + "-" + i;
            if (!exists.test(withSuffix)) {
                return withSuffix;
            }
        }
        return candidate + "-" + System.currentTimeMillis();
    }
}
