package com.monteweb.room;

import tools.jackson.databind.JsonNode;
import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class RoomServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Room CRUD ────────────────────────────────────────────────────

    @Test
    void createRoom_shouldReturnCreatedRoom() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Elefanten-Klasse",
                                    "description": "Grundschul-Klasse 3a",
                                    "type": "KLASSE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Elefanten-Klasse"));
    }

    @Test
    void createRoom_missingName_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "No name room"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoom_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unauthorized Room"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Room Details ─────────────────────────────────────────────────

    @Test
    void getRoom_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/rooms/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMyRooms_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/rooms/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Room Update ──────────────────────────────────────────────────

    @Test
    void updateRoom_asCreator_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room
        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Update Test Room", "type": "GRUPPE"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        // Update
        mockMvc.perform(put("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Updated Room Name", "description": "New description"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Room Name"));
    }

    // ── Room Members ─────────────────────────────────────────────────

    @Test
    void getMembers_ofOwnRoom_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room
        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Members Test Room", "type": "GRUPPE"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members").isArray());
    }

    // ── Browse Rooms ─────────────────────────────────────────────────

    @Test
    void browseRooms_shouldReturnPaginatedResults() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/rooms/browse?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void browseRooms_withSearch_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/rooms/browse?q=nonexistent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Join Requests ────────────────────────────────────────────────

    @Test
    void getMyJoinRequests_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/rooms/my-join-requests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void joinRequest_toNonExistentRoom_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/rooms/00000000-0000-0000-0000-000000000099/join-request")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Please let me in"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── Discussion Threads ───────────────────────────────────────────

    @Test
    void getThreads_forOwnRoom_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room
        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Thread Test Room", "type": "GRUPPE"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createThread_asLeader_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room (creator = LEADER)
        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Create Thread Room", "type": "GRUPPE"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Important Discussion",
                                    "content": "Let's discuss this topic"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Important Discussion"));
    }
}
