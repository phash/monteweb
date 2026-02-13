package com.monteweb.admin.internal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateConfigRequest(
        String schoolName,
        String logoUrl,
        BigDecimal targetHoursPerFamily,
        BigDecimal targetCleaningHours,
        String bundesland,
        List<Map<String, String>> schoolVacations
) {
}
