package com.monteweb.calendar;

import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class CalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getEvents_unauthenticated_shouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/calendar/events")
                        .param("from", "2025-01-01")
                        .param("to", "2025-12-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEvents_authenticated_shouldReturnEvents() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-get@example.com", "Calendar", "Getter");

        mockMvc.perform(get("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2025-01-01")
                        .param("to", "2025-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void createSchoolEvent_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-create@example.com", "Calendar", "Creator");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "School Assembly",
                                    "description": "Monthly assembly",
                                    "allDay": true,
                                    "startDate": "%s",
                                    "endDate": "%s",
                                    "scope": "SCHOOL",
                                    "recurrence": "NONE"
                                }
                                """.formatted(today, tomorrow)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("School Assembly"));
    }

    @Test
    void createRoomEvent_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-room@example.com", "Calendar", "Room");

        // Create a room first
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Calendar Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Room Meeting",
                                    "allDay": false,
                                    "startDate": "%s",
                                    "startTime": "14:00",
                                    "endDate": "%s",
                                    "endTime": "15:00",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "recurrence": "NONE"
                                }
                                """.formatted(today, today, roomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Room Meeting"));
    }

    @Test
    void createEvent_missingTitle_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-invalid@example.com", "Calendar", "Invalid");

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "",
                                    "allDay": true,
                                    "startDate": "2025-06-01",
                                    "endDate": "2025-06-01",
                                    "scope": "SCHOOL"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void rsvp_shouldSucceed() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-rsvp@example.com", "Calendar", "RSVP");

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // Create event
        var eventResult = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "RSVP Event",
                                    "allDay": true,
                                    "startDate": "%s",
                                    "endDate": "%s",
                                    "scope": "SCHOOL",
                                    "recurrence": "NONE"
                                }
                                """.formatted(today, today)))
                .andReturn();
        String eventId = TestHelper.parseResponse(eventResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // RSVP
        mockMvc.perform(post("/api/v1/calendar/events/" + eventId + "/rsvp")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ATTENDING"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getRoomEvents_shouldReturnList() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-roomev@example.com", "Calendar", "RoomEv");

        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Events Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/calendar/rooms/" + roomId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getEvent_notFound_shouldReturn404() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc,
                "cal-404@example.com", "Calendar", "NotFound");

        mockMvc.perform(get("/api/v1/calendar/events/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
