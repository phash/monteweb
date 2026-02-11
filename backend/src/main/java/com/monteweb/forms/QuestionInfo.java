package com.monteweb.forms;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QuestionInfo(
        UUID id,
        QuestionType type,
        String label,
        String description,
        boolean required,
        int sortOrder,
        List<String> options,
        Map<String, Object> ratingConfig
) {
}
