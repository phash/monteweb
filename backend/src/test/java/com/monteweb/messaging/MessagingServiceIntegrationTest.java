package com.monteweb.messaging;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class MessagingServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Conversations ────────────────────────────────────────────────

    @Test
    void getConversations_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getConversations_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isUnauthorized());
    }

    // ── Start Conversation ───────────────────────────────────────────

    @Test
    void startConversation_shouldWork() throws Exception {
        // Register two users
        JsonNode user1 = TestHelper.registerAndGetResponse(mockMvc,
                "msg-sender@example.com", "Sender", "User");
        JsonNode user2 = TestHelper.registerAndGetResponse(mockMvc,
                "msg-receiver@example.com", "Receiver", "User");

        String senderToken = user1.path("data").path("accessToken").asText();
        String receiverId = user2.path("data").path("userId").asText();

        mockMvc.perform(post("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "participantIds": ["%s"],
                                    "subject": "Hallo!",
                                    "message": "Erste Nachricht"
                                }
                                """.formatted(receiverId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void startConversation_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/messages/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantIds": ["some-id"], "message": "Hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Send Message ─────────────────────────────────────────────────

    @Test
    void sendMessage_toNonExistentConversation_shouldReturn4xx() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/messages/conversations/00000000-0000-0000-0000-000000000099/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Hello?"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    // ── Get Messages ─────────────────────────────────────────────────

    @Test
    void getMessages_nonExistentConversation_shouldReturn4xx() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/messages/conversations/00000000-0000-0000-0000-000000000099/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getMessages_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/messages/conversations/00000000-0000-0000-0000-000000000099/messages"))
                .andExpect(status().isUnauthorized());
    }

    // ── Unread Count ─────────────────────────────────────────────────

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/messages/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").isNumber());
    }

    // ── Mark as Read ─────────────────────────────────────────────────

    @Test
    void markAsRead_nonExistentConversation_shouldReturn4xx() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/messages/conversations/00000000-0000-0000-0000-000000000099/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    // ── Full Messaging Flow ──────────────────────────────────────────

    @Test
    void fullFlow_createConversation_sendMessage_shouldWork() throws Exception {
        JsonNode user1 = TestHelper.registerAndGetResponse(mockMvc,
                "flow-sender@example.com", "Flow", "Sender");
        JsonNode user2 = TestHelper.registerAndGetResponse(mockMvc,
                "flow-receiver@example.com", "Flow", "Receiver");

        String senderToken = user1.path("data").path("accessToken").asText();
        String receiverId = user2.path("data").path("userId").asText();

        // Create conversation
        var convResult = mockMvc.perform(post("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "participantIds": ["%s"],
                                    "subject": "Flow Test",
                                    "message": "First message"
                                }
                                """.formatted(receiverId)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode convJson = TestHelper.parseResponse(convResult.getResponse().getContentAsString());
        String conversationId = convJson.path("data").path("id").asText();

        // Send follow-up message
        mockMvc.perform(post("/api/v1/messages/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Follow-up message"}
                                """))
                .andExpect(status().isOk());

        // Get messages
        mockMvc.perform(get("/api/v1/messages/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + senderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
