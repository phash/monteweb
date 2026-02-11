package com.monteweb.school;

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
class SchoolSectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── GET /sections ────────────────────────────────────────────────

    @Test
    void getSections_authenticated_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/sections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getSections_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /sections (Admin only) ──────────────────────────────────

    @Test
    void createSection_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/sections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Neue Abteilung", "displayOrder": 99}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSection_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/sections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unauthorized Section"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /sections/{id} (Admin only) ──────────────────────────────

    @Test
    void updateSection_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(put("/api/v1/sections/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Renamed Section"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /sections/{id} (Admin only) ───────────────────────────

    @Test
    void deleteSection_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(delete("/api/v1/sections/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteSection_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/v1/sections/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }
}
