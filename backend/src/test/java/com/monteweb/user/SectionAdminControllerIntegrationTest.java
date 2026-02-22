package com.monteweb.user;

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
class SectionAdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Helper: login as seed user ────────────────────────────────────

    private String loginAs(String email) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "test1234"}
                                """.formatted(email)))
                .andReturn();
        JsonNode json = TestHelper.parseResponse(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    // ── GET /section-admin/my-sections ────────────────────────────────

    @Test
    void mySections_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/section-admin/my-sections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mySections_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sa-regular@example.com", "Regular", "User");

        mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void mySections_sectionAdmin_shouldReturnAssignedSections() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Kinderhaus (Krippe & Kindergarten)"));
    }

    @Test
    void mySections_superAdmin_shouldReturnAllSections() throws Exception {
        String token = loginAs("admin@monteweb.local");

        mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(4));
    }

    // ── GET /section-admin/sections/{id}/users ────────────────────────

    @Test
    void sectionUsers_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/section-admin/sections/00000000-0000-0000-0000-000000000001/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sectionUsers_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sa-users-regular@example.com", "Regular", "User2");

        mockMvc.perform(get("/api/v1/section-admin/sections/00000000-0000-0000-0000-000000000001/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void sectionUsers_sectionAdmin_ownSection_shouldReturnUsers() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        // First get the section ID
        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JsonNode sections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());
        String sectionId = sections.path("data").path(0).path("id").asText();

        mockMvc.perform(get("/api/v1/section-admin/sections/" + sectionId + "/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void sectionUsers_sectionAdmin_otherSection_shouldReturn403() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        // Get all sections via superadmin to find one the section admin doesn't manage
        String adminToken = loginAs("admin@monteweb.local");
        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        JsonNode allSections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());

        // Find a section that is NOT kinderhaus
        String otherSectionId = null;
        for (JsonNode s : allSections.path("data")) {
            if (!s.path("slug").asText().equals("kinderhaus")) {
                otherSectionId = s.path("id").asText();
                break;
            }
        }

        if (otherSectionId != null) {
            mockMvc.perform(get("/api/v1/section-admin/sections/" + otherSectionId + "/users")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /section-admin/sections/{id}/rooms ────────────────────────

    @Test
    void sectionRooms_sectionAdmin_ownSection_shouldReturnRooms() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JsonNode sections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());
        String sectionId = sections.path("data").path(0).path("id").asText();

        mockMvc.perform(get("/api/v1/section-admin/sections/" + sectionId + "/rooms")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void sectionRooms_sectionAdmin_otherSection_shouldReturn403() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        String adminToken = loginAs("admin@monteweb.local");
        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        JsonNode allSections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());

        String otherSectionId = null;
        for (JsonNode s : allSections.path("data")) {
            if (!s.path("slug").asText().equals("kinderhaus")) {
                otherSectionId = s.path("id").asText();
                break;
            }
        }

        if (otherSectionId != null) {
            mockMvc.perform(get("/api/v1/section-admin/sections/" + otherSectionId + "/rooms")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    // ── POST /section-admin/rooms ─────────────────────────────────────

    @Test
    void createRoom_sectionAdmin_ownSection_shouldSucceed() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JsonNode sections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());
        String sectionId = sections.path("data").path(0).path("id").asText();

        mockMvc.perform(post("/api/v1/section-admin/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test-Raum SA",
                                    "description": "Created by section admin test",
                                    "type": "KLASSE",
                                    "sectionId": "%s"
                                }
                                """.formatted(sectionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test-Raum SA"));
    }

    @Test
    void createRoom_sectionAdmin_otherSection_shouldReturn403() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        String adminToken = loginAs("admin@monteweb.local");
        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        JsonNode allSections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());

        String otherSectionId = null;
        for (JsonNode s : allSections.path("data")) {
            if (!s.path("slug").asText().equals("kinderhaus")) {
                otherSectionId = s.path("id").asText();
                break;
            }
        }

        if (otherSectionId != null) {
            mockMvc.perform(post("/api/v1/section-admin/rooms")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Forbidden Room",
                                        "type": "KLASSE",
                                        "sectionId": "%s"
                                    }
                                    """.formatted(otherSectionId)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void createRoom_missingName_shouldReturn422() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        var sectionsResult = mockMvc.perform(get("/api/v1/section-admin/my-sections")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        JsonNode sections = TestHelper.parseResponse(sectionsResult.getResponse().getContentAsString());
        String sectionId = sections.path("data").path(0).path("id").asText();

        mockMvc.perform(post("/api/v1/section-admin/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "KLASSE", "sectionId": "%s"}
                                """.formatted(sectionId)))
                .andExpect(status().is4xxClientError());
    }

    // ── POST /section-admin/users/{id}/special-roles ──────────────────

    @Test
    void assignSpecialRole_invalidRole_shouldFail() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        mockMvc.perform(post("/api/v1/section-admin/users/00000000-0000-0000-0000-000000000001/special-roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "SUPERADMIN"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void assignSpecialRole_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sa-assign-regular@example.com", "Regular", "Assigner");

        mockMvc.perform(post("/api/v1/section-admin/users/00000000-0000-0000-0000-000000000001/special-roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "PUTZORGA"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /section-admin/users/{id}/special-roles/{role} ─────────

    @Test
    void removeSpecialRole_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sa-remove-regular@example.com", "Regular", "Remover");

        mockMvc.perform(delete("/api/v1/section-admin/users/00000000-0000-0000-0000-000000000001/special-roles/PUTZORGA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeSpecialRole_invalidRole_shouldFail() throws Exception {
        String token = loginAs("sectionadmin@monteweb.local");

        mockMvc.perform(delete("/api/v1/section-admin/users/00000000-0000-0000-0000-000000000001/special-roles/SUPERADMIN")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }
}
