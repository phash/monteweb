package com.monteweb.admin;

import java.util.List;
import java.util.Map;

/**
 * Public API: Reduced tenant configuration safe for unauthenticated access.
 * Excludes sensitive fields like githubRepo, internal business config, etc.
 */
public record PublicTenantConfigInfo(
        String schoolName,
        String logoUrl,
        Map<String, Object> theme,
        Map<String, Boolean> modules,
        boolean multilanguageEnabled,
        String defaultLanguage,
        List<String> availableLanguages,
        boolean requireUserApproval,
        String maintenanceMessage,
        String jitsiServerUrl
) {
    public static PublicTenantConfigInfo from(TenantConfigInfo full) {
        return new PublicTenantConfigInfo(
                full.schoolName(),
                full.logoUrl(),
                full.theme(),
                full.modules(),
                full.multilanguageEnabled(),
                full.defaultLanguage(),
                full.availableLanguages(),
                full.requireUserApproval(),
                full.maintenanceMessage(),
                full.jitsiServerUrl()
        );
    }
}
