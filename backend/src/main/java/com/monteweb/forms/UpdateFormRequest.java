package com.monteweb.forms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record UpdateFormRequest(
        @Size(max = 300) String title,
        String description,
        LocalDate deadline,
        @Valid List<QuestionRequest> questions
) {
}
