package com.monteweb.cleaning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Published when a Putzaktion (one-time cleaning action with specificDate) is created.
 * The jobboard module listens to this event to create a corresponding job.
 */
public record PutzaktionCreatedEvent(
        UUID configId,
        UUID sectionId,
        UUID roomId,
        String title,
        String description,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int maxParticipants,
        BigDecimal hoursCredit,
        UUID calendarEventId,
        UUID createdBy
) {
}
