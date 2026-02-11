package com.monteweb.forms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SubmitResponseRequest(
        @NotEmpty @Valid List<AnswerRequest> answers
) {
}
