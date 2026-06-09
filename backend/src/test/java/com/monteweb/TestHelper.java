package com.monteweb;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Helper methods for integration tests.
 */
public class TestHelper {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static int counter = 0;

    /**
     * Registers a new user with a unique email and returns the access token.
     */
    public static String registerAndGetToken(MockMvc mockMvc) throws Exception {
        return registerAndGetToken(mockMvc, "testuser" + (++counter) + "@example.com",
                "Test", "User" + counter);
    }

    /**
     * Registers a user with specific details and returns the access token.
     */
    public static String registerAndGetToken(MockMvc mockMvc, String email,
                                              String firstName, String lastName) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "SecurePass123!",
                                    "firstName": "%s",
                                    "lastName": "%s"
                                }
                                """.formatted(email, firstName, lastName)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(responseBody);
        return json.path("data").path("accessToken").asText();
    }

    /**
     * Registers a user and returns the full response JSON.
     */
    public static JsonNode registerAndGetResponse(MockMvc mockMvc, String email,
                                                   String firstName, String lastName) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "%s",
                                    "password": "SecurePass123!",
                                    "firstName": "%s",
                                    "lastName": "%s"
                                }
                                """.formatted(email, firstName, lastName)))
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /**
     * Extracts JSON from response body.
     */
    public static JsonNode parseResponse(String responseBody) throws Exception {
        return objectMapper.readTree(responseBody);
    }

    /**
     * Logs in with the given credentials and returns the access token.
     * Use for the Flyway seed accounts (e.g. admin@monteweb.local / test1234 — see
     * {@link #loginAsAdmin}, lehrer@monteweb.local / test1234).
     */
    public static String loginAndGetToken(MockMvc mockMvc, String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.path("data").path("accessToken").asText();
    }

    /**
     * Logs in as the seeded SUPERADMIN and returns the access token.
     *
     * <p>NOTE: although V032 seeds admin@monteweb.local with "admin123", a later
     * migration (V111) resets that account's password to "test1234". In the test
     * database (all Flyway migrations applied) the effective admin password is therefore
     * "test1234", not "admin123".
     */
    public static String loginAsAdmin(MockMvc mockMvc) throws Exception {
        return loginAndGetToken(mockMvc, "admin@monteweb.local", "test1234");
    }

    /**
     * Registers a new user with a unique email, promotes them to TEACHER via the admin
     * role endpoint, and returns their access token.
     *
     * <p>Room/post/form/thread creation is restricted to staff (TEACHER/SECTION_ADMIN/
     * SUPERADMIN); the registration default is PARENT. Use this helper for test setup
     * that needs to create such resources. The returned token still authenticates the
     * user — the role gates read the role from the DB, not the JWT, so the promotion
     * takes effect for the existing token.
     */
    public static String registerTeacherAndGetToken(MockMvc mockMvc) throws Exception {
        return registerTeacherAndGetToken(mockMvc, "teacher" + (++counter) + "@example.com",
                "Teacher", "User" + counter);
    }

    /**
     * Registers a user with specific details, promotes them to TEACHER, and returns
     * their access token. See {@link #registerTeacherAndGetToken(MockMvc)}.
     */
    public static String registerTeacherAndGetToken(MockMvc mockMvc, String email,
                                                     String firstName, String lastName) throws Exception {
        JsonNode response = registerAndGetResponse(mockMvc, email, firstName, lastName);
        String token = response.path("data").path("accessToken").asText();
        String userId = response.path("data").path("userId").asText();
        promoteToTeacher(mockMvc, userId);
        return token;
    }

    /**
     * Promotes the given user to TEACHER using a fresh SUPERADMIN session.
     */
    public static void promoteToTeacher(MockMvc mockMvc, String userId) throws Exception {
        String adminToken = loginAsAdmin(mockMvc);
        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "TEACHER"}
                                """))
                .andReturn();
    }
}
