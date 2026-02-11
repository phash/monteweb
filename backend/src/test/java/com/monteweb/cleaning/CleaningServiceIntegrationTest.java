package com.monteweb.cleaning;

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
class CleaningServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Slots ────────────────────────────────────────────────────────

    @Test
    void getSlots_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/cleaning/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getSlots_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/slots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMySlots_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/cleaning/slots/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getMySlots_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/slots/mine"))
                .andExpect(status().isUnauthorized());
    }

    // ── Slot Registration ────────────────────────────────────────────

    @Test
    void registerForSlot_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/cleaning/slots/00000000-0000-0000-0000-000000000099/register")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void unregisterFromSlot_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(delete("/api/v1/cleaning/slots/00000000-0000-0000-0000-000000000099/register")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── Check-in / Check-out ─────────────────────────────────────────

    @Test
    void checkin_nonExistentSlot_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/cleaning/slots/00000000-0000-0000-0000-000000000099/checkin")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"qrToken": "some-token"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkout_nonExistentSlot_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/cleaning/slots/00000000-0000-0000-0000-000000000099/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── Admin Endpoints ──────────────────────────────────────────────

    @Test
    void getConfigs_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/cleaning/configs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getConfigs_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/configs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createConfig_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/cleaning/configs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Putzplan Grundschule",
                                    "description": "Wöchentlicher Putzplan",
                                    "durationMinutes": 120
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/cleaning/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generateSlots_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/cleaning/configs/00000000-0000-0000-0000-000000000001/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"from": "2026-03-01", "to": "2026-03-31"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void getQrCodes_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/cleaning/configs/00000000-0000-0000-0000-000000000001/qr-codes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
