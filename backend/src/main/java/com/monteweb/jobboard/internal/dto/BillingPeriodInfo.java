package com.monteweb.jobboard.internal.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record BillingPeriodInfo(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Instant closedAt,
        UUID closedBy,
        String notes,
        Instant createdAt
) {
}
