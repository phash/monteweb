package com.monteweb.room;

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
class DiscussionThreadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String createRoomAndGetId(String token) throws Exception {
        var result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Discussion Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    void getThreads_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/00000000-0000-0000-0000-000000000001/threads"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getThreads_authenticated_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "disc-list@example.com", "Disc", "List");

        String roomId = createRoomAndGetId(token);

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createThread_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "disc-create@example.com", "Disc", "Creator");

        String roomId = createRoomAndGetId(token);

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Thread",
                                    "content": "Thread content here"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Thread"));
    }

    @Test
    void getThread_shouldReturnThread() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "disc-get@example.com", "Disc", "Getter");

        String roomId = createRoomAndGetId(token);

        var threadResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Get Thread", "content": "Content"}
                                """))
                .andReturn();
        String threadId = TestHelper.parseResponse(threadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/threads/" + threadId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Get Thread"));
    }

    @Test
    void addReply_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "disc-reply@example.com", "Disc", "Replier");

        String roomId = createRoomAndGetId(token);

        var threadResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Reply Thread", "content": "Content"}
                                """))
                .andReturn();
        String threadId = TestHelper.parseResponse(threadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads/" + threadId + "/replies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "This is a reply"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("This is a reply"));
    }

    @Test
    void getReplies_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "disc-replies@example.com", "Disc", "Replies");

        String roomId = createRoomAndGetId(token);

        var threadResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Replies Thread", "content": "Content"}
                                """))
                .andReturn();
        String threadId = TestHelper.parseResponse(threadResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/threads/" + threadId + "/replies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
