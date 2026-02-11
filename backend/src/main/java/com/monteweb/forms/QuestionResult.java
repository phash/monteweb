package com.monteweb.forms;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record QuestionResult(
        UUID questionId,
        String label,
        QuestionType type,
        int totalAnswers,
        Map<String, Integer> optionCounts,
        Double averageRating,
        Map<Integer, Integer> ratingDistribution,
        List<String> textAnswers,
        int yesCount,
        int noCount
) {
}
