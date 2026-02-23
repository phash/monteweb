package com.monteweb.tasks.internal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveTaskRequest(
        @NotNull UUID columnId,
        @NotNull Integer position
) {
}
