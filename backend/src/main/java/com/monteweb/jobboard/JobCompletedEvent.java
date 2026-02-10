package com.monteweb.jobboard;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when a job assignment is completed and confirmed.
 */
public record JobCompletedEvent(
        UUID jobId,
        String jobTitle,
        UUID userId,
        String userName,
        UUID familyId,
        BigDecimal hours
) {
}
