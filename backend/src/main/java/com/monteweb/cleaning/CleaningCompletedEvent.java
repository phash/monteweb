package com.monteweb.cleaning;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published when a cleaning session is completed (checked out).
 */
public record CleaningCompletedEvent(
        UUID slotId,
        UUID userId,
        String userName,
        UUID familyId,
        BigDecimal hoursCredit,
        int actualMinutes
) {
}
