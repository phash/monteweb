package com.monteweb.forms;

import java.util.List;
import java.util.UUID;

public record IndividualAnswer(
        UUID questionId,
        String questionLabel,
        QuestionType type,
        String text,
        List<String> selectedOptions,
        Integer rating
) {
}
