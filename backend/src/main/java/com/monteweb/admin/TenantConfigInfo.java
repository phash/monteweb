package com.monteweb.admin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Public API: Read-only tenant configuration for cross-module use.
 */
public record TenantConfigInfo(
        UUID id,
        String schoolName,
        String logoUrl,
        Map<String, Object> theme,
        Map<String, Boolean> modules,
        BigDecimal targetHoursPerFamily
) {
}
