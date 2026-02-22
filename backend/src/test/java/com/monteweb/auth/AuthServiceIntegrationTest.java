package com.monteweb.auth;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class AuthServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Registration Validation ──────────────────────────────────────

    @Test
    void register_missingEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "password": "SecurePass123!",
                                    "firstName": "Test",
                                    "lastName": "User"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_invalidEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "not-an-email",
                                    "password": "SecurePass123!",
                                    "firstName": "Test",
                                    "lastName": "User"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shortPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "shortpw@example.com",
                                    "password": "short",
                                    "firstName": "Test",
                                    "lastName": "User"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingFirstName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "nofirst@example.com",
                                    "password": "SecurePass123!",
                                    "lastName": "User"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingLastName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "nolast@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Test"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_emptyBody_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_responseContainsUserId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "userid-check@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Check",
                                    "lastName": "UserId"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("userid-check@example.com"));
    }

    // ── Login Edge Cases ─────────────────────────────────────────────

    @Test
    void login_nonExistentUser_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "nonexistent@example.com",
                                    "password": "SomePass123!"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_emptyPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_missingFields_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Refresh Token Flow ───────────────────────────────────────────

    @Test
    void refresh_invalidToken_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "invalid-token-value"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refresh_emptyToken_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": ""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_missingToken_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── Logout ───────────────────────────────────────────────────────

    @Test
    void logout_shouldSucceed() throws Exception {
        // Register and get tokens
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "logout-test@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Logout",
                                    "lastName": "Test"
                                }
                                """))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode json = TestHelper.parseResponse(response);
        String refreshToken = json.path("data").path("refreshToken").asText();

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void logout_afterLogout_refreshShouldFail() throws Exception {
        // Register
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "logout-refresh@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Logout",
                                    "lastName": "Refresh"
                                }
                                """))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode json = TestHelper.parseResponse(response);
        String refreshToken = json.path("data").path("refreshToken").asText();

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"));

        // Try to use revoked refresh token
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().is4xxClientError());
    }

    // ── Password Reset ───────────────────────────────────────────────

    @Test
    void passwordReset_shouldAcceptAnyEmail() throws Exception {
        // Should not reveal whether email exists
        mockMvc.perform(post("/api/v1/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "unknown-user@example.com"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void passwordReset_invalidEmail_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void passwordResetConfirm_invalidToken_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "invalid-reset-token",
                                    "newPassword": "NewSecurePass123!"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void passwordResetConfirm_shortPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "some-token",
                                    "newPassword": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── Full Login-Refresh Flow ──────────────────────────────────────

    @Test
    void fullFlow_register_login_refresh_shouldWork() throws Exception {
        String email = "fullflow@example.com";

        // Register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "SecurePass123!",
                                    "firstName": "Full",
                                    "lastName": "Flow"
                                }
                                """.formatted(email)))
                .andExpect(status().isCreated());

        // Login
        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "SecurePass123!"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String refreshToken = TestHelper.parseResponse(loginResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Refresh
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }
}
