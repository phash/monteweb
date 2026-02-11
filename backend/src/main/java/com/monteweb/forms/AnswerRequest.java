package com.monteweb.forms;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record AnswerRequest(
        @NotNull UUID questionId,
        String text,
        List<String> selectedOptions,
        Integer rating
) {
}
