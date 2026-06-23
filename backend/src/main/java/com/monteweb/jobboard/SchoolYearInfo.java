package com.monteweb.jobboard;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public DTO: a selectable school year (backed by a billing period).
 */
public record SchoolYearInfo(
        UUID id,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {
}
