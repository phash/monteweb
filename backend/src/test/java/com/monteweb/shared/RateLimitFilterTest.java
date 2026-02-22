package com.monteweb.shared;

import com.monteweb.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
@TestPropertySource(properties = "monteweb.rate-limit.enabled=true")
class RateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginEndpoint_withinLimit_shouldNotBeBlocked() throws Exception {
        // First request should succeed (even with bad credentials → 401, not 429)
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "192.168.99.1")
                        .content("""
                                {"email": "rate-test@example.com", "password": "SomePass123!"}
                                """))
                .andExpect(status().isUnprocessableEntity()); // BusinessException → 422
    }

    @Test
    void registerEndpoint_withinLimit_shouldNotBeBlocked() throws Exception {
        // Register with unique IP → should not be rate-limited
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
                .andExpect(status().is2xxSuccessful()); // 200 or 201, not 429
    }

    @Test
    void nonAuthEndpoint_shouldNotBeRateLimited() throws Exception {
        // Health endpoint is not subject to rate limiting
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }
}
