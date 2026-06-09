package com.monteweb.room;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class RoomControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Logs in as a seeded account (password {@code test1234}) and returns its access token.
     * Used to obtain a TEACHER token, since room creation (US-050) is restricted to
     * TEACHER / SECTION_ADMIN / SUPERADMIN.
     */
    private String loginAs(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "test1234"}
                                """.formatted(email)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    void getMyRooms_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyRooms_authenticated_shouldReturnEmptyList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "room-mine@example.com", "Room", "User");

        mockMvc.perform(get("/api/v1/rooms/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createRoom_asTeacher_shouldSucceed() throws Exception {
        String token = loginAs("lehrer@monteweb.local");

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test Room", "type": "PROJEKT", "description": "A test project room"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Room"))
                .andExpect(jsonPath("$.data.type").value("PROJEKT"));
    }

    @Test
    void createRoom_asParent_shouldReturn403() throws Exception {
        // US-050: PARENT must not be able to create rooms. Freshly registered
        // users default to UserRole.PARENT.
        String token = TestHelper.registerAndGetToken(mockMvc,
                "room-parent-create@example.com", "Parent", "Creator");

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Forbidden Room", "type": "PROJEKT"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoom_asStudent_shouldReturn403() throws Exception {
        // US-050: STUDENT must not be able to create rooms.
        String token = loginAs("schueler@monteweb.local");

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Forbidden Room", "type": "PROJEKT"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoom_blankName_shouldFail() throws Exception {
        String token = loginAs("lehrer@monteweb.local");

        mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "type": "PROJEKT"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getRoom_shouldReturnRoom() throws Exception {
        String token = loginAs("lehrer@monteweb.local");

        // Create a room first
        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Retrievable Room", "type": "GRUPPE"}
                                """))
                .andReturn();

        String roomId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Get the room
        mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Retrievable Room"));
    }

    @Test
    void browseRooms_shouldReturnRooms() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "room-browse@example.com", "Room", "Browser");

        mockMvc.perform(get("/api/v1/rooms/browse")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createJoinRequest_shouldSucceed() throws Exception {
        // Create room as user A (teacher — only staff may create rooms)
        String tokenA = loginAs("lehrer@monteweb.local");
        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "JoinRequest Room", "type": "PROJEKT"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Request join as user B
        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "room-joiner@example.com", "Room", "Joiner");

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/join-request")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Please let me join"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyJoinRequests_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "room-myjr@example.com", "My", "JR");

        mockMvc.perform(get("/api/v1/rooms/my-join-requests")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void discoverRooms_shouldReturnPagedResults() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "room-discover@example.com", "Room", "Discoverer");

        mockMvc.perform(get("/api/v1/rooms/discover")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getRoomMembers_asMember_shouldSucceed() throws Exception {
        String token = loginAs("lehrer@monteweb.local");

        var createResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Members Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // GET /rooms/{id} returns detail response with members array for room members
        mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members").isArray());
    }
}
