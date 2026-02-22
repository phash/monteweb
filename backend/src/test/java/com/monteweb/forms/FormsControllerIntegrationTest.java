package com.monteweb.forms;

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
class FormsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getForms_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/forms"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getForms_authenticated_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "forms-list@example.com", "Forms", "User");

        mockMvc.perform(get("/api/v1/forms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getMyForms_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "forms-mine@example.com", "Forms", "Mine");

        mockMvc.perform(get("/api/v1/forms/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createForm_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "forms-create@example.com", "Forms", "Creator");

        // Create a room first (user becomes LEADER, which grants create permission for ROOM scope)
        String roomId = createRoom(token, "Create Form Room");

        mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Survey",
                                    "description": "A test survey",
                                    "type": "SURVEY",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "anonymous": false,
                                    "questions": [
                                        {
                                            "type": "TEXT",
                                            "label": "What is your name?",
                                            "required": true
                                        },
                                        {
                                            "type": "SINGLE_CHOICE",
                                            "label": "Favorite color?",
                                            "required": false,
                                            "options": ["Red", "Blue", "Green"]
                                        }
                                    ]
                                }
                                """.formatted(roomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.form.title").value("Test Survey"));
    }

    @Test
    void getForm_notFound_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "forms-404@example.com", "Forms", "NotFound");

        // Forms service throws IllegalArgumentException("Form not found") which maps to 400
        mockMvc.perform(get("/api/v1/forms/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createAndGetForm_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "forms-getone@example.com", "Forms", "GetOne");

        // Create room first
        String roomId = createRoom(token, "GetOne Form Room");

        var createResult = mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Retrievable Survey",
                                    "type": "SURVEY",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "anonymous": true,
                                    "questions": [
                                        {
                                            "type": "YES_NO",
                                            "label": "Do you agree?",
                                            "required": true
                                        }
                                    ]
                                }
                                """.formatted(roomId)))
                .andReturn();
        String formId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("form").path("id").asText();

        mockMvc.perform(get("/api/v1/forms/" + formId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.form.title").value("Retrievable Survey"));
    }

    @Test
    void publishForm_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "forms-publish@example.com", "Forms", "Publisher");

        // Create room first
        String roomId = createRoom(token, "Publish Form Room");

        var createResult = mockMvc.perform(post("/api/v1/forms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Publishable Survey",
                                    "type": "CONSENT",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "anonymous": false,
                                    "questions": [
                                        {
                                            "type": "YES_NO",
                                            "label": "Do you consent?",
                                            "required": true
                                        }
                                    ]
                                }
                                """.formatted(roomId)))
                .andReturn();
        String formId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("form").path("id").asText();

        mockMvc.perform(post("/api/v1/forms/" + formId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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
}
