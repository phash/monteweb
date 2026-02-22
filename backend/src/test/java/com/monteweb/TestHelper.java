package com.monteweb;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
}
