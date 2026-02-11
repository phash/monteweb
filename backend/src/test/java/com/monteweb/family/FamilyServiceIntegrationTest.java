package com.monteweb.family;

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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class FamilyServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Create Family ────────────────────────────────────────────────

    @Test
    void createFamily_shouldReturnFamilyWithInviteCode() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Familie Müller"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Familie Müller"));
    }

    @Test
    void createFamily_missingName_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFamily_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unauthorized Family"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Get My Families ──────────────────────────────────────────────

    @Test
    void getMyFamilies_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/families/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMyFamilies_afterCreating_shouldIncludeFamily() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create family
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Check Family"}
                                """))
                .andExpect(status().isOk());

        // Verify it appears
        mockMvc.perform(get("/api/v1/families/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // ── Invite Code Flow ─────────────────────────────────────────────

    @Test
    void generateInviteCode_shouldReturnCode() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create family
        var createResult = mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Invite Code Family"}
                                """))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String familyId = json.path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/families/" + familyId + "/invite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void joinByInviteCode_invalidCode_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/families/join")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode": "INVALID-CODE-12345"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    // ── Hours Account ────────────────────────────────────────────────

    @Test
    void getHours_shouldReturnHourSummary() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create family
        var createResult = mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hours Family"}
                                """))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String familyId = json.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/families/" + familyId + "/hours")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Family Invitations ───────────────────────────────────────────

    @Test
    void getMyInvitations_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/families/my-invitations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMyInvitations_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/families/my-invitations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptInvitation_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/families/invitations/00000000-0000-0000-0000-000000000099/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void declineInvitation_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/families/invitations/00000000-0000-0000-0000-000000000099/decline")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
