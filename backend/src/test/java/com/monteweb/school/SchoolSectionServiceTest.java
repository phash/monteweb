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
class SchoolSectionServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SchoolModuleApi schoolModuleApi;

    @Test
    void findAllActive_shouldReturnDefaultSections() {
        var sections = schoolModuleApi.findAllActive();
        // Default sections are seeded via Flyway
        org.assertj.core.api.Assertions.assertThat(sections).isNotNull();
    }

    @Test
    void getSections_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/sections"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getSections_authenticated_shouldReturnSections() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "section-test@example.com", "Section", "Test");

        mockMvc.perform(get("/api/v1/sections")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createSection_withoutAdmin_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "section-nonadmin@example.com", "NonAdmin", "User");

        mockMvc.perform(post("/api/v1/sections")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test Section", "description": "Desc", "sortOrder": 99}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void findById_withValidId_shouldReturnSection() {
        var sections = schoolModuleApi.findAllActive();
        if (!sections.isEmpty()) {
            var found = schoolModuleApi.findById(sections.get(0).id());
            org.assertj.core.api.Assertions.assertThat(found).isPresent();
        }
    }
}
