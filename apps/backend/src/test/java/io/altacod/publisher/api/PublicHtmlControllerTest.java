package io.altacod.publisher.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.altacod.publisher.api.dto.PublicPostDetailDto;
import io.altacod.publisher.api.dto.PublicPostSummaryDto;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicHtmlControllerTest {

    private final PublicPostService posts = mock(PublicPostService.class);
    private final WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
    private final PublicHtmlController controller =
            new PublicHtmlController(posts, workspaces,
                    new ObjectMapper().findAndRegisterModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS),
                    "https://myposts.pro");
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

    @Test
    void landingHasIndexableTextAndMetadataWithoutJavaScript() throws Exception {
        var response = controller.landing(request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("<html lang=\"ru\">")
                .contains("<h1>Ваш текст сначала принадлежит вам.</h1>")
                .contains("<link rel=\"canonical\" href=\"https://myposts.pro/\">")
                .contains("\"@type\":\"WebSite\"")
                .contains("href=\"/assets/app.css\"")
                .contains("src=\"/assets/app.js\"");
    }

    @Test
    void blogPageKeepsPaginationAndEscapesAuthorContent() throws Exception {
        when(workspaces.existsBySlug("writer")).thenReturn(true);
        var summary = new PublicPostSummaryDto(
                1, "<Writer & Co>", "post", "Описание", Instant.parse("2026-01-02T10:00:00Z"),
                null, null, null, "Превью", 2, "Автор", null);
        when(posts.listPublished("writer", PageRequest.of(1, 20))).thenReturn(
                new PageImpl<>(List.of(summary), PageRequest.of(1, 20), 60));

        var response = controller.blog("writer", "2", request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("href=\"https://myposts.pro/blog/writer?page=2\"")
                .contains("&lt;Writer &amp; Co&gt;")
                .contains("href=\"/blog/writer/p/post\"")
                .contains("window.__PUBLISHER_BOOTSTRAP__=")
                .contains("\"page\":1");
        assertThat(response.getBody()).doesNotContain("<Writer & Co>");
    }

    @Test
    void articleUsesRealDataAndSanitizesInitialHtml() throws Exception {
        var detail = new PublicPostDetailDto(
                1, "Статья <новая>", "post", "Описание",
                "<p>Текст</p><script>alert(1)</script><img src=\"/safe.jpg\" onerror=\"alert(2)\">",
                null, List.of(), List.of(), Instant.parse("2026-01-02T10:00:00Z"),
                Instant.parse("2026-01-03T10:00:00Z"), 2, "Автор", null);
        when(posts.getPublished("writer", "post")).thenReturn(detail);

        var response = controller.article("writer", "post", request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("<h1>Статья &lt;новая&gt;</h1>")
                .contains("<p>Текст</p>")
                .contains("src=\"/safe.jpg\"")
                .contains("\"@type\":\"BlogPosting\"")
                .contains("href=\"https://myposts.pro/blog/writer/p/post\"");
        assertThat(response.getBody()).doesNotContain("<script>alert(1)</script>", "onerror=");
    }

    @Test
    void nonPublicArticleIsNotIndexedOrRendered() throws Exception {
        when(posts.getPublished("writer", "private")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        var response = controller.article("writer", "private", request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .contains("content=\"noindex,nofollow\"")
                .contains("Материал не найден")
                .doesNotContain("BlogPosting");
    }

    @Test
    void unknownBlogReturnsNoindex404() throws Exception {
        var response = controller.blog("missing", null, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("content=\"noindex,nofollow\"");
    }
}
