package com.monteweb.family;

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
class FamilyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createFamily_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fam-create@example.com", "Family", "Creator");

        mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Test Family"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Family"));
    }

    @Test
    void getMyFamilies_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fam-mine@example.com", "Family", "Mine");

        mockMvc.perform(get("/api/v1/families/mine")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void generateInviteCode_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fam-invite@example.com", "Family", "Invite");

        // Create family first
        var createResult = mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Invite Family"}
                                """))
                .andReturn();
        String familyId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Generate invite code
        mockMvc.perform(post("/api/v1/families/" + familyId + "/invite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteCode").isNotEmpty());
    }

    @Test
    void joinByInviteCode_shouldAddMember() throws Exception {
        String tokenA = TestHelper.registerAndGetToken(mockMvc,
                "fam-ownerA@example.com", "FamOwner", "A");

        // Create family
        var createResult = mockMvc.perform(post("/api/v1/families")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Join Family"}
                                """))
                .andReturn();
        String familyId = TestHelper.parseResponse(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Get invite code
        var inviteResult = mockMvc.perform(post("/api/v1/families/" + familyId + "/invite")
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn();
        String inviteCode = TestHelper.parseResponse(inviteResult.getResponse().getContentAsString())
                .path("data").path("inviteCode").asText();

        // Join as user B
        String tokenB = TestHelper.registerAndGetToken(mockMvc,
                "fam-joinerB@example.com", "Joiner", "B");

        mockMvc.perform(post("/api/v1/families/join")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"inviteCode": "%s"}
                                """.formatted(inviteCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createFamily_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/families")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Unauth Family"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyInvitations_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fam-myinv@example.com", "Family", "MyInv");

        mockMvc.perform(get("/api/v1/families/my-invitations")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getAll_nonAdmin_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "fam-nonAdmin@example.com", "NonAdmin", "Family");

        mockMvc.perform(get("/api/v1/families")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void familyModuleApi_findByUserId_shouldWork() {
        // Module API test through interface
        var families = org.springframework.test.util.ReflectionTestUtils
                .getField(this, "familyModuleApi");
        // Basic null check - the API should be autowirable
        org.assertj.core.api.Assertions.assertThat(familyModuleApi).isNotNull();
    }

    @Autowired
    private FamilyModuleApi familyModuleApi;

    @Test
    void familyModuleApi_findAll_shouldReturn() {
        var families = familyModuleApi.findAll();
        org.assertj.core.api.Assertions.assertThat(families).isNotNull();
    }
}
