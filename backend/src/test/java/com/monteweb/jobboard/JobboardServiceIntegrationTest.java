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
                                    "hours": 2.0,
                                    "date": "2026-06-15",
                                    "maxParticipants": 3,
                                    "category": "GENERAL"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Schulhof aufräumen"));
    }

    @Test
    void createJob_missingTitle_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "No title job",
                                    "hours": 2.0
                                }
                                """))
                .andExpect(status().isBadRequest());
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

        // Create job
        var createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Detail Job",
                                    "description": "Job for detail test",
                                    "hours": 1.5,
                                    "date": "2026-07-01",
                                    "maxParticipants": 2,
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
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Create job
        var createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Apply Test Job",
                                    "description": "Job for apply test",
                                    "hours": 3.0,
                                    "date": "2026-07-15",
                                    "maxParticipants": 5,
                                    "category": "GENERAL"
                                }
                                """))
                .andReturn();

        JsonNode json = TestHelper.parseResponse(createResult.getResponse().getContentAsString());
        String jobId = json.path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + token))
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

        mockMvc.perform(get("/api/v1/jobs/my-jobs")
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

    // ── Report (Admin) ───────────────────────────────────────────────

    @Test
    void getReport_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/report")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReportPdf_regularUser_shouldReturn403() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/jobs/report/pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
