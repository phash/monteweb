package com.monteweb.admin;

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
class AdminConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Public Config Endpoint ───────────────────────────────────────

    @Test
    void getPublicConfig_authenticated_shouldReturnConfig() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schoolName").isNotEmpty())
                .andExpect(jsonPath("$.data.modules").isMap());
    }

    // ── Admin Config Endpoints (require SUPERADMIN) ──────────────────

    @Test
    void getAdminConfig_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAdminConfig_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/admin/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateConfig_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/admin/config")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolName": "Hacked School"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTheme_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/admin/config/theme")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"primaryColor": "#ff0000"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateModules_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/admin/config/modules")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messaging": false}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void uploadLogo_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(multipart("/api/v1/admin/config/logo")
                        .file("file", "fake-image-data".getBytes())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLog_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/admin/audit-log")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAuditLog_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-log"))
                .andExpect(status().isUnauthorized());
    }

    // ── Actuator Endpoints ───────────────────────────────────────────

    @Test
    void actuatorHealth_shouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheus_shouldBePublic() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
