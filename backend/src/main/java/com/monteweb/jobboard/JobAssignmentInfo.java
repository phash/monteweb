package com.monteweb.jobboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record JobAssignmentInfo(
        UUID id,
        UUID jobId,
        String jobTitle,
        UUID userId,
        String userName,
        UUID familyId,
        String familyName,
        AssignmentStatus status,
        BigDecimal actualHours,
        boolean confirmed,
        UUID confirmedBy,
        Instant confirmedAt,
        String notes,
        Instant assignedAt,
        Instant completedAt
) {
}
