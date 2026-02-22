package com.monteweb.notification;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class NotificationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationModuleApi notificationModuleApi;

    @Test
    void getNotifications_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotifications_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "notif-get@example.com", "Notification", "User");

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void readAllNotifications_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "notif-readall@example.com", "Notification", "ReadAll");

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void notificationModuleApi_shouldBeInjectable() {
        assertThat(notificationModuleApi).isNotNull();
    }

    @Test
    void notificationType_shouldHaveExpectedValues() {
        // Verify the enum contains expected types
        assertThat(NotificationType.values()).isNotEmpty();
        assertThat(NotificationType.valueOf("MESSAGE")).isNotNull();
    }
}
