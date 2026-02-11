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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class FormsServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── GET /forms ───────────────────────────────────────────────────

    @Test
    void getForms_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/forms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getForms_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/forms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getForms_withPagination_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/forms?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── GET /forms/mine ──────────────────────────────────────────────

    @Test
    void getMyForms_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/forms/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyForms_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/forms/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /forms (Create) ─────────────────────────────────────────

    @Test
    void createForm_shouldReturnCreatedForm() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Feedback Formular",
                                    "description": "Eltern-Feedback zum Schuljahr",
                                    "type": "SURVEY",
                                    "fields": [
                                        {"label": "Zufriedenheit", "type": "RATING", "required": true},
                                        {"label": "Kommentar", "type": "TEXT", "required": false}
                                    ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Feedback Formular"));
    }

    @Test
    void createForm_missingTitle_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description": "No title"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createForm_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/forms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Unauthorized Form"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /forms/{id} ──────────────────────────────────────────────

    @Test
    void getForm_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/forms/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getForm_existing_shouldReturnDetails() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create form
        var createResult = mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Detail Test Form",
                                    "type": "SURVEY",
                                    "fields": [{"label": "Name", "type": "TEXT", "required": true}]
                                }
                                """))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String formId = json.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/forms/" + formId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Detail Test Form"));
    }

    // ── Publish Form ─────────────────────────────────────────────────

    @Test
    void publishForm_shouldUpdateStatus() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create room first (for scope)
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Forms Room"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(roomResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        // Create form
        var createResult = mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Publish Test Form",
                                    "type": "SURVEY",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "fields": [{"label": "Rating", "type": "RATING", "required": true}]
                                }
                                """.formatted(roomId)))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String formId = json.path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void publishForm_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/forms/00000000-0000-0000-0000-000000000099/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
