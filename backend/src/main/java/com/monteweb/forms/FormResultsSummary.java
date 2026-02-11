package com.monteweb.forms;

import java.util.List;

public record FormResultsSummary(
        FormInfo form,
        List<QuestionResult> results
) {
}
