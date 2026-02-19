package com.monteweb.jobboard;

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
class JobboardServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Job Listing ──────────────────────────────────────────────────

    @Test
    void listJobs_shouldReturnPaginatedResults() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listJobs_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    // ── Create Job ───────────────────────────────────────────────────

    @Test
    void createJob_shouldReturnCreatedJob() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Schulhof aufräumen",
                                    "description": "Laub rechen und Müll sammeln",
                                    "estimatedHours": 2.0,
                                    "scheduledDate": "2026-06-15",
                                    "maxAssignees": 3,
                                    "category": "GENERAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Schulhof aufräumen"));
    }

    @Test
    void createJob_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Unauthorized Job"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── Get Job Detail ───────────────────────────────────────────────

    @Test
    void getJob_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void getJob_existing_shouldReturnDetails() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        var createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Detail Job",
                                    "description": "Job for detail test",
                                    "estimatedHours": 1.5,
                                    "scheduledDate": "2026-07-01",
                                    "maxAssignees": 2,
                                    "category": "GENERAL"
                                }
                                """))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String jobId = json.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Detail Job"));
    }

    // ── Apply for Job ────────────────────────────────────────────────

    @Test
    void applyForJob_shouldSucceed() throws Exception {
        // User A creates the job
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "job-creator-apply@example.com", "Job", "Creator");

        var createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Apply Test Job",
                                    "description": "Job for apply test",
                                    "estimatedHours": 3.0,
                                    "scheduledDate": "2026-07-15",
                                    "maxAssignees": 5,
                                    "category": "GENERAL"
                                }
                                """))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String jobId = json.path("data").path("id").asText();

        // User B applies for the job (different user than creator)
        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "job-applier@example.com", "Job", "Applier");

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
    }

    @Test
    void applyForJob_nonExistent_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/jobs/00000000-0000-0000-0000-000000000099/apply")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ── My Jobs / Assignments ────────────────────────────────────────

    @Test
    void getMyAssignments_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/my-assignments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getMyJobs_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Categories ───────────────────────────────────────────────────

    @Test
    void getCategories_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── Report ───────────────────────────────────────────────────────

    @Test
    void getReport_authenticatedUser_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getReportPdf_authenticatedUser_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/report/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
