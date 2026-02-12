package com.monteweb.feed;

import com.fasterxml.jackson.databind.JsonNode;
import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class FeedServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Feed ─────────────────────────────────────────────────────────

    @Test
    void getFeed_authenticated_shouldReturnPaginatedFeed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/feed?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getFeed_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().isUnauthorized());
    }

    // ── Create Post ──────────────────────────────────────────────────

    @Test
    void createPost_shouldReturnCreatedPost() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room first
        String roomId = createRoom(token, "Feed Post Room");

        mockMvc.perform(post("/api/v1/feed/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "Willkommen im neuen Schuljahr!",
                                    "title": "Schulstart",
                                    "sourceType": "ROOM",
                                    "sourceId": "%s",
                                    "parentOnly": false
                                }
                                """.formatted(roomId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.content").value("Willkommen im neuen Schuljahr!"));
    }

    @Test
    void createPost_emptyContent_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/feed/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPost_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/feed/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Unauthorized post"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Post Detail ──────────────────────────────────────────────────

    @Test
    void getPost_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/feed/posts/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPost_existing_shouldReturnPost() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room and post
        String roomId = createRoom(token, "Detail Room");
        String postId = createPost(token, roomId, "Detail test post");

        mockMvc.perform(get("/api/v1/feed/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Detail test post"));
    }

    // ── Update Post ──────────────────────────────────────────────────

    @Test
    void updatePost_asAuthor_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room and post
        String roomId = createRoom(token, "Update Room");
        String postId = createPost(token, roomId, "Original content");

        // Update
        mockMvc.perform(put("/api/v1/feed/posts/" + postId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Updated content"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated content"));
    }

    // ── Delete Post ──────────────────────────────────────────────────

    @Test
    void deletePost_asAuthor_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room and post
        String roomId = createRoom(token, "Delete Room");
        String postId = createPost(token, roomId, "Delete me");

        mockMvc.perform(delete("/api/v1/feed/posts/" + postId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deletePost_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(delete("/api/v1/feed/posts/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── Comments ─────────────────────────────────────────────────────

    @Test
    void addComment_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room and post
        String roomId = createRoom(token, "Comment Room");
        String postId = createPost(token, roomId, "Comment on me");

        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Great post!"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void addComment_emptyContent_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/feed/posts/00000000-0000-0000-0000-000000000001/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── Banners ──────────────────────────────────────────────────────

    @Test
    void getBanners_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/feed/banners")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getBanners_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/feed/banners"))
                .andExpect(status().isUnauthorized());
    }

    // ── Pin Post ─────────────────────────────────────────────────────

    @Test
    void pinPost_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/feed/posts/00000000-0000-0000-0000-000000000099/pin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private String createRoom(String token, String name) throws Exception {
        var result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "type": "PROJEKT"}
                                """.formatted(name)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String createPost(String token, String roomId, String content) throws Exception {
        var result = mockMvc.perform(post("/api/v1/feed/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "%s",
                                    "sourceType": "ROOM",
                                    "sourceId": "%s",
                                    "parentOnly": false
                                }
                                """.formatted(content, roomId)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }
}
