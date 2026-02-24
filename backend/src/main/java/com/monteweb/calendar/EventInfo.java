package com.monteweb.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Public API: Read-only calendar event information for cross-module use.
 */
public record EventInfo(
        UUID id,
        String title,
        String description,
        String location,
        boolean allDay,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        EventScope scope,
        UUID scopeId,
        String scopeName,
        EventRecurrence recurrence,
        LocalDate recurrenceEnd,
        boolean cancelled,
        String eventType,
        String color,
        UUID createdBy,
        String creatorName,
        int attendingCount,
        int maybeCount,
        int declinedCount,
        RsvpStatus currentUserRsvp,
        int linkedJobCount,
        Instant createdAt,
        Instant updatedAt
) {
}
