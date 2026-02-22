package com.monteweb.notification;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class NotificationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationModuleApi notificationModule;

    // ── GET /notifications ───────────────────────────────────────────

    @Test
    void getNotifications_authenticated_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void getNotifications_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getNotifications_withPagination_shouldWork() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/notifications?page=0&size=5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(5));
    }

    // ── GET /notifications/unread-count ──────────────────────────────

    @Test
    void getUnreadCount_authenticated_shouldReturnCount() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.count").isNumber());
    }

    @Test
    void getUnreadCount_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /notifications/{id}/read ─────────────────────────────────

    @Test
    void markAsRead_nonExistentId_shouldStillReturn200() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);
        UUID fakeId = UUID.randomUUID();

        // markAsRead uses an UPDATE query, which doesn't throw if no rows matched
        mockMvc.perform(put("/api/v1/notifications/" + fakeId + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/" + UUID.randomUUID() + "/read"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /notifications/read-all ──────────────────────────────────

    @Test
    void markAllAsRead_authenticated_shouldReturn200() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void markAllAsRead_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    // ── NotificationType enum ────────────────────────────────────────

    @Test
    void notificationType_shouldHaveAllExpectedValues() {
        var types = NotificationType.values();
        assertTrue(types.length >= 19, "Should have at least 19 notification types");

        // Verify key types exist
        assertNotNull(NotificationType.valueOf("POST"));
        assertNotNull(NotificationType.valueOf("COMMENT"));
        assertNotNull(NotificationType.valueOf("MESSAGE"));
        assertNotNull(NotificationType.valueOf("SYSTEM"));
        assertNotNull(NotificationType.valueOf("DISCUSSION_THREAD"));
        assertNotNull(NotificationType.valueOf("DISCUSSION_REPLY"));
        assertNotNull(NotificationType.valueOf("EVENT_CREATED"));
        assertNotNull(NotificationType.valueOf("EVENT_CANCELLED"));
        assertNotNull(NotificationType.valueOf("ROOM_JOIN_REQUEST"));
        assertNotNull(NotificationType.valueOf("ROOM_JOIN_APPROVED"));
        assertNotNull(NotificationType.valueOf("ROOM_JOIN_DENIED"));
        assertNotNull(NotificationType.valueOf("FAMILY_INVITATION"));
        assertNotNull(NotificationType.valueOf("FAMILY_INVITATION_ACCEPTED"));
        assertNotNull(NotificationType.valueOf("CLEANING_COMPLETED"));
        assertNotNull(NotificationType.valueOf("JOB_COMPLETED"));
        assertNotNull(NotificationType.valueOf("FORM_PUBLISHED"));
        assertNotNull(NotificationType.valueOf("CONSENT_REQUIRED"));
    }
}
