package io.altacod.publisher.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.altacod.publisher.api.dto.PostPayload;
import io.altacod.publisher.api.dto.RegisterRequest;
import io.altacod.publisher.post.PostStatus;
import io.altacod.publisher.post.PostVisibility;
import io.altacod.publisher.user.UserRepository;
import io.altacod.publisher.workspace.MembershipEntity;
import io.altacod.publisher.workspace.MembershipRepository;
import io.altacod.publisher.workspace.MembershipRole;
import io.altacod.publisher.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostUnpublishIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired MembershipRepository membershipRepository;

    @Test
    void authorCanUnpublishKeepDraftAndPublishAgainButAnotherMemberCannot() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        JsonNode author = register("author-" + suffix + "@example.com", "Author");
        String token = author.path("accessToken").asText();
        JsonNode me = objectMapper.readTree(mockMvc.perform(get("/api/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long workspaceId = me.path("workspaces").get(0).path("id").asLong();
        String slug = me.path("workspaces").get(0).path("slug").asText();
        long authorId = me.path("user").path("id").asLong();
        PostPayload published = new PostPayload(
                "Reusable article", "reusable-article", "Summary", "# Original", "<h1>Original</h1>",
                PostVisibility.PUBLIC, PostStatus.PUBLISHED, null, List.of(), false,
                List.of(), false, null, null
        );
        JsonNode created = objectMapper.readTree(mockMvc.perform(post("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(published)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long postId = created.path("id").asLong();
        String publicUrl = "/api/public/w/" + slug + "/posts/reusable-article";

        mockMvc.perform(get(publicUrl)).andExpect(status().isOk())
                .andExpect(jsonPath("$.authorId").value(authorId));

        JsonNode other = register("other-" + suffix + "@example.com", "Other");
        String otherToken = other.path("accessToken").asText();
        var workspace = workspaceRepository.findById(workspaceId).orElseThrow();
        var otherUser = userRepository.findByEmailIgnoreCase("other-" + suffix + "@example.com").orElseThrow();
        membershipRepository.save(new MembershipEntity(workspace, otherUser, MembershipRole.EDITOR, Instant.now()));
        mockMvc.perform(patch("/api/posts/{id}/unpublish", postId)
                        .header("Authorization", "Bearer " + otherToken)
                        .header("X-Workspace-Id", workspaceId))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(publicUrl)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/posts/{id}/unpublish", postId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.publishedAt").doesNotExist())
                .andExpect(jsonPath("$.bodySource").value("# Original"));
        mockMvc.perform(get(publicUrl)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/posts")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(postId))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
        mockMvc.perform(patch("/api/posts/{id}/unpublish", postId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", workspaceId))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/posts/{id}", postId)
                        .header("Authorization", "Bearer " + token)
                        .header("X-Workspace-Id", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(published)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mockMvc.perform(get(publicUrl)).andExpect(status().isOk());
    }

    private JsonNode register(String email, String name) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(email, "password-123", name))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }
}
