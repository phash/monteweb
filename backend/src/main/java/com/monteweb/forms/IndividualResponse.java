package com.monteweb.forms;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record IndividualResponse(
        UUID responseId,
        UUID userId,
        String userName,
        Instant submittedAt,
        List<IndividualAnswer> answers
) {
}
