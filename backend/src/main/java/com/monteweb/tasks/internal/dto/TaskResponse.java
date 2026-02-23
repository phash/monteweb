package com.monteweb.tasks.internal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID columnId,
        String title,
        String description,
        UUID assigneeId,
        String assigneeName,
        UUID createdBy,
        String createdByName,
        LocalDate dueDate,
        int position,
        Instant createdAt
) {
}
