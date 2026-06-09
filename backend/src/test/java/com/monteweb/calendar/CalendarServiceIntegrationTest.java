package com.monteweb.calendar;

import tools.jackson.databind.JsonNode;
import com.monteweb.TestContainerConfig;
import com.monteweb.TestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
class CalendarServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ── Event Scope Enum ─────────────────────────────────────────────

    @Test
    void eventScope_shouldHaveAllValues() {
        assertEquals(3, EventScope.values().length);
        assertNotNull(EventScope.valueOf("ROOM"));
        assertNotNull(EventScope.valueOf("SECTION"));
        assertNotNull(EventScope.valueOf("SCHOOL"));
    }

    // ── Event Recurrence Enum ────────────────────────────────────────

    @Test
    void eventRecurrence_shouldHaveAllValues() {
        assertEquals(5, EventRecurrence.values().length);
        assertNotNull(EventRecurrence.valueOf("NONE"));
        assertNotNull(EventRecurrence.valueOf("DAILY"));
        assertNotNull(EventRecurrence.valueOf("WEEKLY"));
        assertNotNull(EventRecurrence.valueOf("MONTHLY"));
        assertNotNull(EventRecurrence.valueOf("YEARLY"));
    }

    // ── RSVP Status Enum ─────────────────────────────────────────────

    @Test
    void rsvpStatus_shouldHaveAllValues() {
        assertEquals(3, RsvpStatus.values().length);
        assertNotNull(RsvpStatus.valueOf("ATTENDING"));
        assertNotNull(RsvpStatus.valueOf("MAYBE"));
        assertNotNull(RsvpStatus.valueOf("DECLINED"));
    }

    // ── GET /calendar/events ─────────────────────────────────────────

    @Test
    void getEvents_authenticated_shouldReturnPage() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/calendar/events?from=2026-01-01&to=2026-12-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getEvents_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/calendar/events"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /calendar/events (Create) ───────────────────────────────

    @Test
    void createSchoolEvent_regularUser_shouldFail() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        // Only SUPERADMIN can create SCHOOL scope events
        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "School-wide Event",
                                    "description": "Everyone is invited",
                                    "allDay": true,
                                    "startDate": "2026-06-15",
                                    "endDate": "2026-06-15",
                                    "scope": "SCHOOL",
                                    "recurrence": "NONE"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createEvent_missingTitle_shouldReturn400() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "allDay": true,
                                    "startDate": "2026-06-15",
                                    "endDate": "2026-06-15",
                                    "scope": "SCHOOL"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/calendar/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Unauthorized Event",
                                    "startTime": "2026-06-15T10:00:00Z",
                                    "endTime": "2026-06-15T12:00:00Z",
                                    "scope": "SCHOOL"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /calendar/events/{id} ────────────────────────────────────

    @Test
    void getEvent_nonExistent_shouldReturn4xx() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(get("/api/v1/calendar/events/00000000-0000-0000-0000-000000000099")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getEventById_asRoomMember_shouldSucceed() throws Exception {
        String token = TestHelper.registerTeacherAndGetToken(mockMvc,
                "cal-owner@example.com", "Calendar", "Owner");

        // Create a room (creator becomes LEADER) and a ROOM-scoped event
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "IDOR Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        var eventResult = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Private Room Event",
                                    "allDay": false,
                                    "startDate": "2026-06-20",
                                    "startTime": "10:00",
                                    "endDate": "2026-06-20",
                                    "endTime": "11:00",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "recurrence": "NONE"
                                }
                                """.formatted(roomId)))
                .andReturn();
        String eventId = TestHelper.parseResponse(eventResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Member (the creator/leader) may read the event by id
        mockMvc.perform(get("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Private Room Event"));
    }

    @Test
    void getEventById_asNonMember_shouldBeDenied() throws Exception {
        String ownerToken = TestHelper.registerTeacherAndGetToken(mockMvc,
                "cal-idor-owner@example.com", "Calendar", "IdorOwner");

        // Owner creates a private room and a ROOM-scoped event
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Secret Room", "type": "GRUPPE"}
                                """))
                .andReturn();
        String roomId = TestHelper.parseResponse(roomResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        var eventResult = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Secret Event",
                                    "allDay": false,
                                    "startDate": "2026-06-20",
                                    "startTime": "10:00",
                                    "endDate": "2026-06-20",
                                    "endTime": "11:00",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "recurrence": "NONE"
                                }
                                """.formatted(roomId)))
                .andReturn();
        String eventId = TestHelper.parseResponse(eventResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // A different, non-member user must NOT be able to read the event by id (IDOR)
        String outsiderToken = TestHelper.registerAndGetToken(mockMvc,
                "cal-idor-outsider@example.com", "Calendar", "Outsider");

        mockMvc.perform(get("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().is4xxClientError());
    }

    // ── RSVP ─────────────────────────────────────────────────────────

    @Test
    void rsvp_toNonExistentEvent_shouldReturn4xx() throws Exception {
        String token = TestHelper.registerAndGetToken(mockMvc);

        mockMvc.perform(post("/api/v1/calendar/events/00000000-0000-0000-0000-000000000099/rsvp")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ATTENDING"}
                                """))
                .andExpect(status().is4xxClientError());
    }

    // ── Room Events ──────────────────────────────────────────────────

    @Test
    void getRoomEvents_shouldWork() throws Exception {
        String token = TestHelper.registerTeacherAndGetToken(mockMvc);

        // Create a room first
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Calendar Room", "type": "GRUPPE"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(roomResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/calendar/rooms/" + roomId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── Create Room Event ────────────────────────────────────────────

    // ── Manage permission: only creator or SUPERADMIN (US-105/106/107) ──

    /**
     * Helper: SUPERADMIN logs in and creates a SCHOOL-scoped event, returning its id.
     * SCHOOL events are readable by every authenticated user, which lets us assert that
     * a non-creator who CAN read the event still cannot manage it.
     */
    private String createSchoolEventAsAdmin() throws Exception {
        String adminToken = TestHelper.loginAsAdmin(mockMvc);

        var eventResult = mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Manage Permission Event",
                                    "allDay": true,
                                    "startDate": "2026-06-15",
                                    "endDate": "2026-06-15",
                                    "scope": "SCHOOL",
                                    "recurrence": "NONE"
                                }
                                """))
                .andReturn();
        return TestHelper.parseResponse(eventResult.getResponse().getContentAsString())
                .path("data").path("id").asText();
    }

    @Test
    void deleteEvent_byNonCreator_shouldBeForbidden() throws Exception {
        String eventId = createSchoolEventAsAdmin();

        String otherToken = TestHelper.registerAndGetToken(mockMvc,
                "cal-mng-del@example.com", "Calendar", "NonCreatorDel");

        mockMvc.perform(delete("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelEvent_byNonCreator_shouldBeForbidden() throws Exception {
        String eventId = createSchoolEventAsAdmin();

        String otherToken = TestHelper.registerAndGetToken(mockMvc,
                "cal-mng-cancel@example.com", "Calendar", "NonCreatorCancel");

        mockMvc.perform(post("/api/v1/calendar/events/" + eventId + "/cancel")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateEvent_byNonCreator_shouldBeForbidden() throws Exception {
        String eventId = createSchoolEventAsAdmin();

        String otherToken = TestHelper.registerAndGetToken(mockMvc,
                "cal-mng-update@example.com", "Calendar", "NonCreatorUpdate");

        mockMvc.perform(put("/api/v1/calendar/events/" + eventId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Hijacked Title"}
                                """))
                .andExpect(status().isForbidden());
    }

    // ── Jitsi: rejected when module disabled (US-114) ────────────────

    @Test
    void generateJitsiRoom_whenModuleDisabled_shouldBeForbidden() throws Exception {
        // jitsi DB toggle defaults to false; even the SUPERADMIN creator must be rejected
        String adminToken = TestHelper.loginAsAdmin(mockMvc);

        String eventId = createSchoolEventAsAdmin();

        mockMvc.perform(post("/api/v1/calendar/events/" + eventId + "/jitsi")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRoomEvent_asLeader_shouldWork() throws Exception {
        String token = TestHelper.registerTeacherAndGetToken(mockMvc);

        // Create a room (creator becomes LEADER)
        var roomResult = mockMvc.perform(post("/api/v1/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Event Room Test", "type": "GRUPPE"}
                                """))
                .andReturn();

        JsonNode roomJson = TestHelper.parseResponse(roomResult.getResponse().getContentAsString());
        String roomId = roomJson.path("data").path("id").asText();

        mockMvc.perform(post("/api/v1/calendar/events")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Room Meeting",
                                    "description": "Weekly room meeting",
                                    "allDay": false,
                                    "startDate": "2026-06-20",
                                    "startTime": "10:00",
                                    "endDate": "2026-06-20",
                                    "endTime": "11:00",
                                    "scope": "ROOM",
                                    "scopeId": "%s",
                                    "recurrence": "WEEKLY"
                                }
                                """.formatted(roomId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Room Meeting"));
    }
}
