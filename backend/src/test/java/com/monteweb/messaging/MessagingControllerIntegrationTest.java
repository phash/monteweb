package com.monteweb.messaging;

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
class MessagingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getConversations_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/messages/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getConversations_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "msg-get@example.com", "Messaging", "User");

        mockMvc.perform(get("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void startConversation_shouldSucceed() throws Exception {
        var responseA = TestHelper.registerAndGetResponse(mockMvc,
                "msg-senderA@example.com", "Sender", "A");
        String tokenA = responseA.path("data").path("accessToken").asText();

        var responseB = TestHelper.registerAndGetResponse(mockMvc,
                "msg-receiverB@example.com", "Receiver", "B");
        String userIdB = responseB.path("data").path("userId").asText();

        // Start conversation using correct request format (participantIds list)
        mockMvc.perform(post("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "participantIds": ["%s"],
                                    "isGroup": false
                                }
                                """.formatted(userIdB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "msg-unread@example.com", "Messaging", "Unread");

        mockMvc.perform(get("/api/v1/messages/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").isNumber());
    }

    @Test
    void getConversation_notFound_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "msg-404@example.com", "Messaging", "NotFound");

        mockMvc.perform(get("/api/v1/messages/conversations/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void sendMessage_toExistingConversation_shouldSucceed() throws Exception {
        var responseA = TestHelper.registerAndGetResponse(mockMvc,
                "msg-sendmsgA@example.com", "SendMsg", "A");
        String tokenA = responseA.path("data").path("accessToken").asText();

        var responseB = TestHelper.registerAndGetResponse(mockMvc,
                "msg-sendmsgB@example.com", "SendMsg", "B");
        String userIdB = responseB.path("data").path("userId").asText();

        // Start conversation to get ID
        var convResult = mockMvc.perform(post("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantIds": ["%s"], "isGroup": false}
                                """.formatted(userIdB)))
                .andReturn();

        String convId = TestHelper.parseResponse(convResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Send another message
        mockMvc.perform(post("/api/v1/messages/conversations/" + convId + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content": "Follow-up message"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Follow-up message"));
    }

    @Test
    void markAsRead_shouldSucceed() throws Exception {
        var responseA = TestHelper.registerAndGetResponse(mockMvc,
                "msg-readA@example.com", "ReadA", "User");
        String tokenA = responseA.path("data").path("accessToken").asText();

        var responseB = TestHelper.registerAndGetResponse(mockMvc,
                "msg-readB@example.com", "ReadB", "User");
        String userIdB = responseB.path("data").path("userId").asText();
        String tokenB = responseB.path("data").path("accessToken").asText();

        // Create conversation
        var convResult = mockMvc.perform(post("/api/v1/messages/conversations")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participantIds": ["%s"], "isGroup": false}
                                """.formatted(userIdB)))
                .andReturn();
        String convId = TestHelper.parseResponse(convResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Mark as read from B's perspective
        mockMvc.perform(put("/api/v1/messages/conversations/" + convId + "/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }
}
