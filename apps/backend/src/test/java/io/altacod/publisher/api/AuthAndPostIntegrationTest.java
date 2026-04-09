package io.altacod.publisher.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.api.dto.LoginRequest;
import io.altacod.publisher.api.dto.PostPayload;
import io.altacod.publisher.api.dto.RegisterRequest;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndPostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginCreateAndListPosts() throws Exception {
        String email = "author-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "password-123",
                                "Author"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String body = login.getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("accessToken").asText();

        MvcResult me = mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaces[0].slug").exists())
                .andReturn();

        long workspaceId = objectMapper.readTree(me.getResponse().getContentAsString())
                .path("workspaces").get(0).get("id").asLong();

        PostPayload draft = new PostPayload(
                "First post",
                "",
                "Short",
                "## Hello",
                "<p>Hello</p>",
                PostVisibility.PUBLIC,
                PostStatus.DRAFT,
                null,
                List.of(),
                false,
                null
        );

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", String.valueOf(workspaceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(draft)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("first-post"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", String.valueOf(workspaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        assertThat(token).isNotBlank();
    }

    @Test
    void publicFeedRequiresPublishedPost() throws Exception {
        String email = "reader-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "password-123",
                                "Reader"
                        ))))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "password-123"))))
                .andExpect(status().isOk())
                .andReturn();

        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();
        MvcResult me = mockMvc.perform(get("/api/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        long workspaceId = objectMapper.readTree(me.getResponse().getContentAsString())
                .path("workspaces").get(0).get("id").asLong();
        String slug = objectMapper.readTree(me.getResponse().getContentAsString())
                .path("workspaces").get(0).get("slug").asText();

        PostPayload published = new PostPayload(
                "Live",
                "live",
                "Excerpt",
                "Body",
                "<p>Body</p>",
                PostVisibility.PUBLIC,
                PostStatus.PUBLISHED,
                null,
                List.of(),
                false,
                null
        );

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", String.valueOf(workspaceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(published)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/public/w/"                         + slug + "/posts/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Live"));
    }
}
