package com.monteweb.calendar;

import java.time.LocalDate;
import java.util.List;
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
}
