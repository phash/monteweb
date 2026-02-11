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
class CleaningControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getSlots_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/slots"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSlots_authenticated_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "clean-slots@example.com", "Clean", "Slots");

        mockMvc.perform(get("/api/v1/cleaning/slots")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getMySlots_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "clean-mine@example.com", "Clean", "Mine");

        mockMvc.perform(get("/api/v1/cleaning/slots/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getSlot_notFound_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "clean-404@example.com", "Clean", "NotFound");

        mockMvc.perform(get("/api/v1/cleaning/slots/00000000-0000-0000-0000-000000000001")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getConfigs_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/configs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getConfigs_nonAdmin_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "clean-nonadmin@example.com", "Clean", "NonAdmin");

        mockMvc.perform(get("/api/v1/cleaning/configs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDashboard_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/cleaning/dashboard")
                        .param("sectionId", "00000000-0000-0000-0000-000000000001")
                        .param("from", "2025-01-01")
                        .param("to", "2025-12-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerForSlot_nonexistent_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "clean-register@example.com", "Clean", "Register");

        mockMvc.perform(post("/api/v1/cleaning/slots/00000000-0000-0000-0000-000000000001/register")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }
}
