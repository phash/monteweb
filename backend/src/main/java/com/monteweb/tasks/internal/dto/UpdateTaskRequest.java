package com.monteweb.tasks.internal.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @Size(max = 255) String title,
        String description,
        UUID assigneeId,
        LocalDate dueDate,
        UUID columnId,
        Integer position
) {
}
