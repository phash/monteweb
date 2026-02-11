package com.monteweb.forms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateFormRequest(
        @NotBlank @Size(max = 300) String title,
        String description,
        @NotNull FormType type,
        @NotNull FormScope scope,
        UUID scopeId,
        boolean anonymous,
        LocalDate deadline,
        @Valid List<QuestionRequest> questions
) {
}
