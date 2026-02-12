package com.monteweb.jobboard;

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
class JobboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listJobs_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listJobs_authenticated_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "job-list@example.com", "Job", "Lister");

        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createJob_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "job-create@example.com", "Job", "Creator");

        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Test Job",
                                    "description": "A job for testing",
                                    "category": "GENERAL",
                                    "estimatedHours": 2.0,
                                    "maxAssignees": 3,
                                    "scheduledDate": "2025-07-01",
                                    "scheduledTime": "09:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Job"));
    }

    @Test
    void getJob_shouldReturnJob() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "job-get@example.com", "Job", "Getter");

        var createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Get Job",
                                    "description": "Retrievable job",
                                    "category": "GENERAL",
                                    "estimatedHours": 1.0,
                                    "maxAssignees": 1,
                                    "scheduledDate": "2025-08-01"
                                }
                                """))
                .andReturn();
        String jobId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Get Job"));
    }

    @Test
    void applyForJob_shouldSucceed() throws Exception {
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "job-ownerA@example.com", "JobOwner", "A");

        var createResult = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Apply Job",
                                    "description": "Job to apply for",
                                    "category": "CLEANING",
                                    "estimatedHours": 3.0,
                                    "maxAssignees": 5,
                                    "scheduledDate": "2025-09-01"
                                }
                                """))
                .andReturn();
        String jobId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "job-applierB@example.com", "Applier", "B");

        // User B must belong to a family to apply for jobs
        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Applier Family"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/jobs/" + jobId + "/apply")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyAssignments_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "job-myass@example.com", "Job", "MyAssignments");

        mockMvc.perform(get("/api/v1/jobs/my-assignments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getMyJobs_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "job-mine@example.com", "Job", "Mine");

        mockMvc.perform(get("/api/v1/jobs/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getCategories_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "job-cats@example.com", "Job", "Categories");

        mockMvc.perform(get("/api/v1/jobs/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
