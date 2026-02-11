package com.monteweb.admin.internal.dto;

import java.math.BigDecimal;

public record UpdateConfigRequest(
        String schoolName,
        String logoUrl,
        BigDecimal targetHoursPerFamily,
        BigDecimal targetCleaningHours
) {
}
