package io.altacod.publisher.api;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadAttachAndPublicFile() throws Exception {
        String email = "media-" + System.nanoTime() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                email,
                                "password-123",
                                "Media author"
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

        JsonNode ws = objectMapper.readTree(me.getResponse().getContentAsString()).path("workspaces").get(0);
        long workspaceId = ws.get("id").asLong();
        String workspaceSlug = ws.get("slug").asText();

        MvcResult up = mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile(
                                "file",
                                "pixel.png",
                                "image/png",
                                new byte[]{ (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a }
                        ))
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", String.valueOf(workspaceId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        long mediaId = objectMapper.readTree(up.getResponse().getContentAsString()).get("id").asLong();

        PostPayload published = new PostPayload(
                "With media",
                "with-media",
                "Ex",
                "Src",
                "<p>Hi</p>",
                PostVisibility.PUBLIC,
                PostStatus.PUBLISHED,
                null,
                List.of(),
                false,
                List.of(mediaId),
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", String.valueOf(workspaceId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(published)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.media.length()").value(1))
                .andExpect(jsonPath("$.media[0].mediaAssetId").value((int) mediaId));

        mockMvc.perform(get("/api/public/w/" + workspaceSlug + "/posts/with-media"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media.length()").value(1));

        mockMvc.perform(get("/api/public/w/" + workspaceSlug + "/media/" + mediaId + "/file"))
                .andExpect(status().isOk())
                .andExpect(r -> assertThat(r.getResponse().getContentAsByteArray().length).isPositive());
    }
}
