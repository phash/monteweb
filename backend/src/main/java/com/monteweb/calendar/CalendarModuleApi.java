package com.monteweb.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the calendar module.
 * Other modules can query upcoming events through this interface.
 */
public interface CalendarModuleApi {

    List<EventInfo> getUpcomingEventsForRoom(UUID roomId, LocalDate from, LocalDate to);

    List<EventInfo> getUpcomingEventsForSection(UUID sectionId, LocalDate from, LocalDate to);

    List<EventInfo> getUpcomingEventsForSchool(LocalDate from, LocalDate to);

    Optional<EventInfo> findById(UUID eventId);

    /**
     * Returns events where any of the given user IDs has an RSVP with status ATTENDING or MAYBE.
     * Used by the family calendar to show events for all family members.
     */
    List<EventInfo> getEventsForUserIds(List<UUID> userIds, LocalDate from, LocalDate to);

    /**
     * Creates a calendar event from a system/internal source (e.g., cleaning module).
     * Bypasses normal permission checks since it's system-initiated.
     */
    EventInfo createEventFromSystem(CreateEventRequest request, UUID createdBy);

    /**
     * Searches calendar events by title or description (case-insensitive LIKE).
     * Used by the global search module.
     */
    List<EventInfo> searchEvents(String query, int limit);

    /**
     * DSGVO: Export all calendar-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
