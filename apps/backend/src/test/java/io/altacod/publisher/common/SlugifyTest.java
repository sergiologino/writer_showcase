package io.altacod.publisher.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugifyTest {

    @Test
    void slugifyNormalizesText() {
        assertThat(Slugify.slugify("Hello World")).isEqualTo("hello-world");
        assertThat(Slugify.slugify("  Café démo  ")).isEqualTo("cafe-demo");
    }

    @Test
    void uniqueSlugAppendsSuffixWhenTaken() {
        var slug = Slugify.uniqueSlug("test", candidate -> candidate.equals("test"));
        assertThat(slug).isEqualTo("test-2");
    }
}
