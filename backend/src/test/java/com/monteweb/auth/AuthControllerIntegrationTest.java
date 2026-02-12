package com.monteweb.auth;

import com.monteweb.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void register_shouldCreateUserAndReturnTokens() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Test",
                                    "lastName": "User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void register_duplicateEmail_shouldFail() throws Exception {
        // First registration
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "duplicate@example.com",
                            "password": "SecurePass123!",
                            "firstName": "First",
                            "lastName": "User"
                        }
                        """));

        // Second registration with same email
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "duplicate@example.com",
                                    "password": "AnotherPass456!",
                                    "firstName": "Second",
                                    "lastName": "User"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void login_withValidCredentials_shouldReturnTokens() throws Exception {
        // Register first
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "login-test@example.com",
                            "password": "SecurePass123!",
                            "firstName": "Login",
                            "lastName": "Test"
                        }
                        """));

        // Login
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "login-test@example.com",
                                    "password": "SecurePass123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void login_withInvalidPassword_shouldFail() throws Exception {
        // Register
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "wrong-pass@example.com",
                            "password": "SecurePass123!",
                            "firstName": "Wrong",
                            "lastName": "Pass"
                        }
                        """));

        // Login with wrong password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "wrong-pass@example.com",
                                    "password": "WrongPassword!"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void refresh_withValidToken_shouldReturnNewTokens() throws Exception {
        // Register and get refresh token
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "refresh-test@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Refresh",
                                    "lastName": "Test"
                                }
                                """))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        // Extract refresh token from JSON (simple approach)
        String refreshToken = response.split("\"refreshToken\":\"")[1].split("\"")[0];

        // Refresh
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }
}
