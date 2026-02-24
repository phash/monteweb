package com.monteweb.calendar;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Public API: Request to create a calendar event.
 */
public record CreateEventRequest(
        @NotBlank @Size(max = 300) String title,
        String description,
        @Size(max = 500) String location,
        boolean allDay,
        @NotNull LocalDate startDate,
        LocalTime startTime,
        @NotNull LocalDate endDate,
        LocalTime endTime,
        @NotNull EventScope scope,
        UUID scopeId,
        EventRecurrence recurrence,
        LocalDate recurrenceEnd,
        String eventType,
        String color
) {
}
