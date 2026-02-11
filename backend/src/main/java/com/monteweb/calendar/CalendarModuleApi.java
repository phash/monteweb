package com.monteweb.calendar;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Public API: Facade interface for the calendar module.
 * Other modules can query upcoming events through this interface.
 */
public interface CalendarModuleApi {

    List<EventInfo> getUpcomingEventsForRoom(UUID roomId, LocalDate from, LocalDate to);

    List<EventInfo> getUpcomingEventsForSection(UUID sectionId, LocalDate from, LocalDate to);

    List<EventInfo> getUpcomingEventsForSchool(LocalDate from, LocalDate to);
}
