package io.altacod.publisher.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicArticleHtmlTest {

    @Test
    void keepsArticleFormattingAndRelativeMediaButDropsActiveContent() {
        String result = PublicArticleHtml.clean("""
                <h2>Раздел</h2><p><a href="javascript:alert(1)">опасная ссылка</a></p>
                <img src="/api/public/w/author/media/2/file" alt="Фото" onerror="alert(2)">
                <script>alert(3)</script><iframe src="https://www.youtube.com/embed/example"></iframe>
                """);
        assertThat(result).contains("<h2>Раздел</h2>", "src=\"/api/public/w/author/media/2/file\"", "sandbox=");
        assertThat(result).doesNotContain("javascript:", "onerror=", "<script>");
    }
}
