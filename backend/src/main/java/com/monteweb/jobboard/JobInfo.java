package com.monteweb.jobboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobInfo(
        UUID id,
        String title,
        String description,
        String category,
        String location,
        UUID sectionId,
        BigDecimal estimatedHours,
        int maxAssignees,
        int currentAssignees,
        JobStatus status,
        LocalDate scheduledDate,
        String scheduledTime,
        UUID createdBy,
        String creatorName,
        String contactInfo,
        UUID eventId,
        String eventTitle,
        Instant createdAt
) {
}
