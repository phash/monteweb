package com.monteweb.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public record QuestionRequest(
        @NotNull QuestionType type,
        @NotBlank String label,
        String description,
        boolean required,
        List<String> options,
        Map<String, Object> ratingConfig
) {
}
