package com.monteweb.cleaning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CleaningConfigInfo(
        UUID id,
        UUID sectionId,
        String sectionName,
        UUID roomId,
        String roomName,
        String title,
        String description,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int minParticipants,
        int maxParticipants,
        BigDecimal hoursCredit,
        boolean active,
        LocalDate specificDate,
        UUID calendarEventId,
        UUID jobId,
        String participantCircle,
        UUID participantCircleId
) {
}
