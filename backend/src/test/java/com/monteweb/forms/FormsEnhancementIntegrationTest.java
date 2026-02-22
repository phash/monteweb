package com.monteweb.forms;

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
 * Tests for Issue #87: Forms/Surveys Enhancement
 * - Response editing (PUT /respond, GET /my-response)
 * - Archive endpoint (POST /archive)
 * - Extended delete (creator can delete any status)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class FormsEnhancementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── PUT /forms/{id}/respond (update response) ─────────────────

    @Test
    void updateResponse_shouldSucceedForPublishedFormWithExistingResponse() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "Update Resp Room");
        String formId = createForm(token, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit initial response
        mockMvc.perform(post("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 3}]}
                                """.formatted(getFirstQuestionId(token, formId))))
                .andExpect(status().isOk());

        // Update response
        mockMvc.perform(put("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 5}]}
                                """.formatted(getFirstQuestionId(token, formId))))
                .andExpect(status().isOk());
    }

    @Test
    void updateResponse_shouldFailForAnonymousForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "Anon Resp Room");
        String formId = createAnonymousForm(token, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit initial response
        mockMvc.perform(post("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 3}]}
                                """.formatted(getFirstQuestionId(token, formId))))
                .andExpect(status().isOk());

        // Update should fail (anonymous)
        mockMvc.perform(put("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 5}]}
                                """.formatted(getFirstQuestionId(token, formId))))
                .andExpect(status().isConflict());
    }

    @Test
    void updateResponse_shouldFailForClosedForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "Closed Resp Room");
        String formId = createForm(token, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit initial response
        String questionId = getFirstQuestionId(token, formId);
        mockMvc.perform(post("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 3}]}
                                """.formatted(questionId)))
                .andExpect(status().isOk());

        // Close form
        mockMvc.perform(post("/api/v1/forms/" + formId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Update should fail (closed)
        mockMvc.perform(put("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 5}]}
                                """.formatted(questionId)))
                .andExpect(status().isConflict());
    }

    // ── GET /forms/{id}/my-response ───────────────────────────────

    @Test
    void getMyResponse_shouldReturnAnswersForExistingResponse() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "MyResp Room");
        String formId = createForm(token, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit response
        String questionId = getFirstQuestionId(token, formId);
        mockMvc.perform(post("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 4}]}
                                """.formatted(questionId)))
                .andExpect(status().isOk());

        // Get my response
        mockMvc.perform(get("/api/v1/forms/" + formId + "/my-response")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.responseId").exists())
                .andExpect(jsonPath("$.data.answers").isArray())
                .andExpect(jsonPath("$.data.answers[0].questionId").value(questionId));
    }

    @Test
    void getMyResponse_shouldReturnNullForAnonymousForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "AnonMyResp Room");
        String formId = createAnonymousForm(token, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Submit response
        mockMvc.perform(post("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 3}]}
                                """.formatted(getFirstQuestionId(token, formId))))
                .andExpect(status().isOk());

        // Get my response (should return null data for anonymous)
        mockMvc.perform(get("/api/v1/forms/" + formId + "/my-response")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // ── POST /forms/{id}/archive ──────────────────────────────────

    @Test
    void archiveForm_shouldSucceedForClosedForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "Archive Room");
        String formId = createForm(token, roomId);

        // Publish then close
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/forms/" + formId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Archive
        mockMvc.perform(post("/api/v1/forms/" + formId + "/archive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    void archiveForm_shouldFailForPublishedForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "ArchiveFail Room");
        String formId = createForm(token, roomId);

        // Publish only
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Archive should fail (not closed)
        mockMvc.perform(post("/api/v1/forms/" + formId + "/archive")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    // ── DELETE /forms/{id} (extended) ─────────────────────────────

    @Test
    void deleteForm_creatorCanDeletePublishedForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "Delete Pub Room");
        String formId = createForm(token, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Delete should succeed (creator can delete any status)
        mockMvc.perform(delete("/api/v1/forms/" + formId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deleteForm_creatorCanDeleteClosedForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "Delete Closed Room");
        String formId = createForm(token, roomId);

        // Publish then close
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/forms/" + formId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Delete should succeed (creator can delete any status)
        mockMvc.perform(delete("/api/v1/forms/" + formId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── GET /forms/{id}/results (respondent visibility) ───────────

    @Test
    void getResults_respondentCanSeeClosedFormResults() throws Exception {
        String creatorToken = TestHelper.registerAndGetToken(mockMvc);
        String respondentToken = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(creatorToken, "Results Room");

        // Add respondent to room
        addMemberToRoom(creatorToken, roomId, respondentToken);

        String formId = createForm(creatorToken, roomId);

        // Publish
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // Respondent submits response
        String questionId = getFirstQuestionId(respondentToken, formId);
        mockMvc.perform(post("/api/v1/forms/" + formId + "/respond")
                        .header("Authorization", "Bearer " + respondentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answers": [{"questionId": "%s", "rating": 4}]}
                                """.formatted(questionId)))
                .andExpect(status().isOk());

        // Close form
        mockMvc.perform(post("/api/v1/forms/" + formId + "/close")
                        .header("Authorization", "Bearer " + creatorToken))
                .andExpect(status().isOk());

        // Respondent should be able to see results of closed form
        mockMvc.perform(get("/api/v1/forms/" + formId + "/results")
                        .header("Authorization", "Bearer " + respondentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results").isArray());
    }

    // ── Available forms include CLOSED ─────────────────────────────

    @Test
    void getAvailableForms_shouldIncludeClosedForms() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        String roomId = createRoom(token, "AvailClosed Room");
        String formId = createForm(token, roomId);

        // Publish then close
        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/forms/" + formId + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Available forms should include closed form
        mockMvc.perform(get("/api/v1/forms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── Helpers ──────────────────────────────────────────────────

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

    private String createForm(String token, String roomId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Survey",
                                    "type": "SURVEY",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "anonymous": false,
                                    "questions": [
                                        {"label": "Rating", "type": "RATING", "required": true}
                                    ]
                                }
                                """.formatted(roomId)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("form").path("id").asText();
    }

    private String createAnonymousForm(String token, String roomId) throws Exception {
        var result = mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Anonymous Survey",
                                    "type": "SURVEY",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "anonymous": true,
                                    "questions": [
                                        {"label": "Rating", "type": "RATING", "required": true}
                                    ]
                                }
                                """.formatted(roomId)))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("form").path("id").asText();
    }

    private String getFirstQuestionId(String token, String formId) throws Exception {
        var result = mockMvc.perform(get("/api/v1/forms/" + formId)
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        return TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("questions").get(0).path("id").asText();
    }

    private void addMemberToRoom(String leaderToken, String roomId, String memberToken) throws Exception {
        // Get member user ID from token
        var result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + memberToken))
                .andReturn();
        String userId = TestHelper.parseResponse(result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Add member to room
        mockMvc.perform(post("/api/v1/rooms/" + roomId + "/members")
                        .header("Authorization", "Bearer " + leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": "%s", "role": "MEMBER"}
                                """.formatted(userId)));
    }
}
