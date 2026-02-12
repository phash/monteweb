package com.monteweb.room;

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

/**
 * Integration tests for the role concept refactoring:
 * - JoinPolicy (OPEN/REQUEST/INVITE_ONLY)
 * - Room muting/unmuting
 * - Interest room creation with JoinPolicy
 * - Discussion thread creation permissions
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class RoleConceptIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Helpers ────────────────────────────────────────────────────

    private String createRoomAndGetId(String token, String name, String type) throws Exception {
        var result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "type": "%s"}
                                """.formatted(name, type)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    private String createInterestRoomAndGetId(String token, String name) throws Exception {
        var result = mockMvc.perform(post("/api/v1/rooms/interest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "description": "Interest room for testing", "tags": ["test"]}
                                """.formatted(name)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    // ── JoinPolicy ────────────────────────────────────────────────

    @Test
    void createRoom_shouldHaveDefaultJoinPolicy() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-default-jp@example.com", "Default", "JP");

        var result = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Default Policy Room", "type": "GRUPPE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode room = TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data");
        // Default rooms should have REQUEST join policy
        org.junit.jupiter.api.Assertions.assertEquals("REQUEST", room.path("joinPolicy").asText());
    }

    @Test
    void createInterestRoom_shouldHaveOpenJoinPolicy() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-interest-jp@example.com", "Interest", "JP");

        var result = mockMvc.perform(post("/api/v1/rooms/interest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Open Interest Room", "tags": ["music"]}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode room = TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data");
        org.junit.jupiter.api.Assertions.assertEquals("OPEN", room.path("joinPolicy").asText());
    }

    @Test
    void discoverRooms_shouldReturnOpenRooms() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-discover@example.com", "Discover", "User");

        // Create an interest room (OPEN)
        createInterestRoomAndGetId(token, "Discoverable Room");

        mockMvc.perform(get("/api/v1/rooms/discover")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void joinRoom_openPolicy_shouldSucceed() throws Exception {
        // User A creates interest room (OPEN policy)
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "rc-joinopen-a@example.com", "Creator", "A");
        String roomId = createInterestRoomAndGetId(tokenA, "Open Join Room");

        // User B joins
        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "rc-joinopen-b@example.com", "Joiner", "B");

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/join")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }

    @Test
    void joinRoom_requestPolicy_shouldFail() throws Exception {
        // User A creates a regular room (REQUEST policy by default)
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "rc-joinreq-a@example.com", "Creator", "ReqA");
        String roomId = createRoomAndGetId(tokenA, "Request Only Room", "GRUPPE");

        // User B tries to directly join – should fail
        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "rc-joinreq-b@example.com", "Joiner", "ReqB");

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/join")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void updateInterestFields_shouldChangeJoinPolicy() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-updatejp@example.com", "Update", "JP");
        String roomId = createInterestRoomAndGetId(token, "Changeable Policy Room");

        // Change from OPEN to REQUEST
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/interest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"joinPolicy": "REQUEST", "tags": ["updated"]}
                                """))
                .andExpect(status().isOk());

        // Verify the room now has REQUEST policy
        var getResult = mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roomData = TestHelper.parseResponse(getResult.getResponse().getContentAsString())
                .path("data");
        org.junit.jupiter.api.Assertions.assertEquals("REQUEST", roomData.path("joinPolicy").asText());
    }

    // ── Room Muting ───────────────────────────────────────────────

    @Test
    void muteRoom_asMember_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-mute@example.com", "Mute", "User");
        String roomId = createRoomAndGetId(token, "Mutable Room", "GRUPPE");

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/mute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unmuteRoom_afterMuting_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-unmute@example.com", "Unmute", "User");
        String roomId = createRoomAndGetId(token, "Unmutable Room", "GRUPPE");

        // Mute first
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/mute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Then unmute
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/unmute")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void muteRoom_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/00000000-0000-0000-0000-000000000001/mute"))
                .andExpect(status().isUnauthorized());
    }

    // ── Room Settings (DiscussionMode) ────────────────────────────

    @Test
    void updateSettings_discussionMode_shouldPersist() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-discmode@example.com", "Disc", "Mode");
        String roomId = createRoomAndGetId(token, "Discussion Mode Room", "GRUPPE");

        // Update settings with ANNOUNCEMENTS_ONLY
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chatEnabled": false,
                                    "filesEnabled": false,
                                    "parentSpaceEnabled": false,
                                    "visibility": "MEMBERS_ONLY",
                                    "discussionMode": "ANNOUNCEMENTS_ONLY",
                                    "allowMemberThreadCreation": false,
                                    "childDiscussionEnabled": false
                                }
                                """))
                .andExpect(status().isOk());

        // Verify by getting the room
        var getResult = mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roomData = TestHelper.parseResponse(getResult.getResponse().getContentAsString())
                .path("data");
        org.junit.jupiter.api.Assertions.assertEquals("ANNOUNCEMENTS_ONLY",
                roomData.path("settings").path("discussionMode").asText());
    }

    @Test
    void updateSettings_disableDiscussions_shouldPersist() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-disableddisc@example.com", "Disabled", "Disc");
        String roomId = createRoomAndGetId(token, "No Discussions Room", "GRUPPE");

        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chatEnabled": false,
                                    "filesEnabled": false,
                                    "parentSpaceEnabled": false,
                                    "visibility": "MEMBERS_ONLY",
                                    "discussionMode": "DISABLED",
                                    "allowMemberThreadCreation": false,
                                    "childDiscussionEnabled": false
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void updateSettings_allowMemberThreadCreation_shouldPersist() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-memberthreads@example.com", "Member", "Threads");
        String roomId = createRoomAndGetId(token, "Member Threads Room", "KLASSE");

        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chatEnabled": true,
                                    "filesEnabled": false,
                                    "parentSpaceEnabled": false,
                                    "visibility": "MEMBERS_ONLY",
                                    "discussionMode": "FULL",
                                    "allowMemberThreadCreation": true,
                                    "childDiscussionEnabled": true
                                }
                                """))
                .andExpect(status().isOk());

        var getResult = mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roomData = TestHelper.parseResponse(getResult.getResponse().getContentAsString())
                .path("data");
        org.junit.jupiter.api.Assertions.assertTrue(
                roomData.path("settings").path("allowMemberThreadCreation").asBoolean());
        org.junit.jupiter.api.Assertions.assertTrue(
                roomData.path("settings").path("childDiscussionEnabled").asBoolean());
    }

    // ── Discussion thread in DISABLED mode ────────────────────────

    @Test
    void createThread_inDisabledMode_shouldStillWorkForLeader() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-thread-disabled@example.com", "Thread", "Disabled");
        String roomId = createRoomAndGetId(token, "Disabled Thread Room", "GRUPPE");

        // Disable discussions
        mockMvc.perform(put("/api/v1/rooms/" + roomId + "/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "chatEnabled": false,
                                    "filesEnabled": false,
                                    "parentSpaceEnabled": false,
                                    "visibility": "MEMBERS_ONLY",
                                    "discussionMode": "DISABLED",
                                    "allowMemberThreadCreation": false,
                                    "childDiscussionEnabled": false
                                }
                                """))
                .andExpect(status().isOk());

        // Leader can still create threads
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/threads")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Leader Thread", "content": "Even in disabled mode"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Leader Thread"));
    }

    // ── Join requests with different policies ─────────────────────

    @Test
    void createJoinRequest_requestPolicy_shouldSucceed() throws Exception {
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "rc-jr-creator@example.com", "JR", "Creator");
        String roomId = createRoomAndGetId(tokenA, "Request Policy JR Room", "PROJEKT");

        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "rc-jr-requester@example.com", "JR", "Requester");

        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/join-request")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "I would like to join this project"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void approveJoinRequest_shouldAddMember() throws Exception {
        // Create room
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "rc-approve-creator@example.com", "Approve", "Creator");
        String roomId = createRoomAndGetId(tokenA, "Approve JR Room", "GRUPPE");

        // Request to join
        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "rc-approve-joiner@example.com", "Approve", "Joiner");
        var jrResult = mockMvc.perform(post("/api/v1/rooms/" + roomId + "/join-request")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Please approve me"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String requestId = TestHelper.parseResponse(jrResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Approve as leader
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/join-requests/" + requestId + "/approve")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // Verify user B can now access the room as member
        mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.members").isArray());
    }

    // ── Public vs. member room view ───────────────────────────────

    @Test
    void getRoom_asNonMember_shouldReturnPublicView() throws Exception {
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "rc-public-creator@example.com", "Public", "Creator");
        String roomId = createRoomAndGetId(tokenA, "Public View Room", "GRUPPE");

        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "rc-public-viewer@example.com", "Public", "Viewer");

        var result = mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roomData = TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data");
        // Public view should have joinPolicy but NOT members array
        org.junit.jupiter.api.Assertions.assertTrue(roomData.has("joinPolicy"));
        org.junit.jupiter.api.Assertions.assertFalse(roomData.has("members"));
    }

    @Test
    void getRoom_asMember_shouldReturnDetailView() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "rc-detail-member@example.com", "Detail", "Member");
        String roomId = createRoomAndGetId(token, "Detail View Room", "GRUPPE");

        var result = mockMvc.perform(get("/api/v1/rooms/" + roomId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode roomData = TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data");
        // Member view should have both settings and members
        org.junit.jupiter.api.Assertions.assertTrue(roomData.has("members"));
        org.junit.jupiter.api.Assertions.assertTrue(roomData.has("settings"));
    }
}
