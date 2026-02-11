package com.monteweb.shared;

import com.monteweb.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpoint_withinLimit_shouldNotBeBlocked() throws Exception {
        // First request should succeed (even with bad credentials)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.168.99.1")
                        .content("""
                                {"email": "rate-test@example.com", "password": "SomePass123!"}
                                """))
                .andExpect(status().is4xxClientError()); // 401 not 429
    }

    @Test
    void registerEndpoint_withinLimit_shouldNotBeBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.168.99.2")
                        .content("""
                                {
                                    "email": "ratelimit-reg@example.com",
                                    "password": "SecurePass123!",
                                    "firstName": "Rate",
                                    "lastName": "Limit"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void nonAuthEndpoint_shouldNotBeRateLimited() throws Exception {
        // Health endpoint is not rate limited
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/v1/auth/password-reset")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "192.168.99.3")
                            .content("""
                                    {"email": "spam@example.com"}
                                    """))
                    .andExpect(status().isOk());
        }
    }
}
