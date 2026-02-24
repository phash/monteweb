package com.monteweb.calendar;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Public API: Request to update a calendar event. All fields are optional.
 */
public record UpdateEventRequest(
        @Size(max = 300) String title,
        String description,
        @Size(max = 500) String location,
        Boolean allDay,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        EventRecurrence recurrence,
        LocalDate recurrenceEnd,
        String color
) {
}
