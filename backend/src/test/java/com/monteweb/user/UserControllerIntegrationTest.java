package com.monteweb.user;

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
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── GET /users/me ────────────────────────────────────────────────

    @Test
    void getMe_authenticated_shouldReturnProfile() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "me-test@example.com", "Profile", "Test");

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("me-test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Profile"))
                .andExpect(jsonPath("$.data.lastName").value("Test"));
    }

    @Test
    void getMe_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /users/me ────────────────────────────────────────────────

    @Test
    void updateMe_shouldUpdateProfile() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "update-me@example.com", "Before", "Update");

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "firstName": "After",
                                    "lastName": "Updated",
                                    "phone": "+49123456789"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("After"))
                .andExpect(jsonPath("$.data.lastName").value("Updated"));
    }

    @Test
    void updateMe_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Hacker"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /users/search ────────────────────────────────────────────

    @Test
    void searchUsers_shouldFindByName() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "searchable-user@example.com", "Searchable", "Person");

        mockMvc.perform(get("/api/v1/users/search?q=Searchable")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void searchUsers_emptyQuery_shouldReturnResults() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/users/search?q=")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void searchUsers_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/search?q=test"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /users/{id} ──────────────────────────────────────────────

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        // Register a user and extract their ID
        JsonNode response = TestHelper.registerAndGetResponse(mockMvc,
                "byid-user@example.com", "ById", "User");
        String userId = response.path("data").path("userId").asText();
        String token = response.path("data").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/" + userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("ById"));
    }

    @Test
    void getUserById_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/users/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── GET /users (Admin) ───────────────────────────────────────────

    @Test
    void listUsers_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /users/{id}/roles (Admin) ────────────────────────────────

    @Test
    void updateRoles_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/users/00000000-0000-0000-0000-000000000001/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "SUPERADMIN"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ── GET /users/me/data-export (DSGVO) ────────────────────────────

    @Test
    void dataExport_authenticated_shouldReturnData() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "dsgvo-export@example.com", "DSGVO", "Export");

        mockMvc.perform(get("/api/v1/users/me/data-export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void dataExport_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/data-export"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /users/me (DSGVO) ─────────────────────────────────────

    @Test
    void deleteMe_authenticated_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "delete-me@example.com", "Delete", "Me");

        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMe_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
