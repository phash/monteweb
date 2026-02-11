package com.monteweb.jobboard;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Hours summary for a family. Used in reports and family view.
 */
public record FamilyHoursInfo(
        UUID familyId,
        String familyName,
        BigDecimal targetHours,
        BigDecimal completedHours,
        BigDecimal pendingHours,
        BigDecimal cleaningHours,
        BigDecimal totalHours,
        BigDecimal remainingHours,
        String trafficLight
) {
}
