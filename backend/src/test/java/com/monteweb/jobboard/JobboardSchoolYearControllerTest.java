package com.monteweb.jobboard;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class JobboardSchoolYearControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void schoolYears_returnsOkForAuthenticatedUser() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sy-test@example.com", "SchoolYear", "Test");
        mockMvc.perform(get("/api/v1/jobs/school-years")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void schoolYears_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/school-years"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void familyHours_withPeriodId_returnsOkOrNotFound() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sy-hours@example.com", "SchoolYear", "Hours");
        // No family exists for this user, so 403 or 404 is expected — but not 400/500
        // The important thing: the endpoint accepts periodId without 400 Bad Request
        mockMvc.perform(get("/api/v1/jobs/family/" + java.util.UUID.randomUUID() + "/hours")
                        .param("periodId", java.util.UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 403 && status != 404) {
                        throw new AssertionError("Expected 403 or 404 but got " + status);
                    }
                });
    }

    @Test
    void familyAssignments_withPeriodId_returnsOkOrNotFound() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "sy-asgn@example.com", "SchoolYear", "Asgn");
        // No family exists for this user, so 403 is expected — but not 400/500
        mockMvc.perform(get("/api/v1/jobs/family/" + java.util.UUID.randomUUID() + "/assignments")
                        .param("periodId", java.util.UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 403 && status != 404) {
                        throw new AssertionError("Expected 403 or 404 but got " + status);
                    }
                });
    }
}
