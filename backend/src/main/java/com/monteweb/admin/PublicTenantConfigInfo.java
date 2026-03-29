package com.monteweb.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    // All DB-managed module toggles are safe to expose (just boolean on/off).
    // Sensitive infrastructure config (LDAP credentials, GitHub PAT, etc.) is in
    // separate TenantConfigInfo fields already excluded from this record.
    private static final Set<String> EXCLUDED_MODULES = Set.of(
        "clamav", "ldap"
    );

    public static PublicTenantConfigInfo from(TenantConfigInfo full) {
        Map<String, Boolean> publicModules = full.modules() != null
            ? full.modules().entrySet().stream()
                .filter(e -> !EXCLUDED_MODULES.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
            : Map.of();

        return new PublicTenantConfigInfo(
                full.schoolName(),
                full.logoUrl(),
                full.theme(),
                publicModules,
                full.multilanguageEnabled(),
                full.defaultLanguage(),
                full.availableLanguages(),
                full.requireUserApproval(),
                full.maintenanceMessage(),
                full.jitsiServerUrl()
        );
    }
}
