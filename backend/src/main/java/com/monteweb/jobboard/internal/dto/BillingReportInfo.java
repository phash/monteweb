package com.monteweb.jobboard.internal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record BillingReportInfo(
        BillingPeriodInfo period,
        List<FamilyBillingEntry> families,
        BillingSummary summary
) {
    public record FamilyBillingEntry(
            UUID familyId,
            String familyName,
            List<FamilyMember> members,
            BigDecimal jobHours,
            BigDecimal cleaningHours,
            BigDecimal totalHours,
            BigDecimal targetHours,
            BigDecimal balance,
            BigDecimal targetCleaningHours,
            BigDecimal cleaningBalance,
            String trafficLight
    ) {
    }

    public record FamilyMember(
            UUID userId,
            String displayName,
            String role
    ) {
    }

    public record BillingSummary(
            int totalFamilies,
            BigDecimal averageHours,
            BigDecimal totalHoursAll,
            long greenCount,
            long yellowCount,
            long redCount
    ) {
    }
}
