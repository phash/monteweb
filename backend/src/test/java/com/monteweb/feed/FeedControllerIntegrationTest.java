package com.monteweb.feed;

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
class FeedControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getFeed_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/feed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getFeed_authenticated_shouldReturnFeed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "feed-get@example.com", "Feed", "User");

        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createPost_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "feed-post@example.com", "Feed", "Poster");

        // Create a room first to use as source
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Feed Room", "type": "PROJEKT"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/feed/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Post",
                                    "content": "This is a test post content",
                                    "sourceType": "ROOM",
                                    "sourceId": "%s",
                                    "parentOnly": false
                                }
                                """.formatted(roomId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Test Post"))
                .andExpect(jsonPath("$.data.content").value("This is a test post content"));
    }

    @Test
    void getBanners_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "feed-banner@example.com", "Feed", "Banner");

        mockMvc.perform(get("/api/v1/feed/banners")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createPost_blankContent_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "feed-blank@example.com", "Feed", "Blank");

        mockMvc.perform(post("/api/v1/feed/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "",
                                    "sourceType": "SCHOOL",
                                    "parentOnly": false
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getPost_notFound_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "feed-404@example.com", "Feed", "NotFound");

        mockMvc.perform(get("/api/v1/feed/posts/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void addComment_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "feed-comment@example.com", "Feed", "Commenter");

        // Create room and post
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Comment Room", "type": "PROJEKT"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        var postResult = mockMvc.perform(post("/api/v1/feed/posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "content": "Post for comments",
                                    "sourceType": "ROOM",
                                    "sourceId": "%s",
                                    "parentOnly": false
                                }
                                """.formatted(roomId)))
                .andReturn();
        String postId = TestHelper.parseResponse(postResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/feed/posts/" + postId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Great post!"}
                                """))
                .andExpect(status().isCreated());
    }
}
