package com.monteweb.admin;

import java.math.BigDecimal;
import java.util.List;
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
        BigDecimal targetHoursPerFamily,
        BigDecimal targetCleaningHours,
        boolean parentToParentMessaging,
        boolean studentToStudentMessaging,
        String bundesland,
        List<Map<String, String>> schoolVacations,
        String githubRepo,
        boolean githubPatConfigured,
        boolean requireAssignmentConfirmation,
        boolean multilanguageEnabled,
        String defaultLanguage,
        List<String> availableLanguages,
        boolean requireUserApproval,
        String privacyPolicyText,
        String privacyPolicyVersion,
        String termsText,
        String termsVersion,
        Integer dataRetentionDaysNotifications,
        Integer dataRetentionDaysAudit,
        String schoolFullName,
        String schoolAddress,
        String schoolPrincipal,
        String techContactName,
        String techContactEmail,
        String twoFactorMode,
        java.time.Instant twoFactorGraceDeadline,
        boolean directoryAdminOnly,
        // LDAP/AD fields (password never exposed)
        boolean ldapEnabled,
        String ldapUrl,
        String ldapBaseDn,
        String ldapBindDn,
        String ldapUserSearchFilter,
        String ldapAttrEmail,
        String ldapAttrFirstName,
        String ldapAttrLastName,
        String ldapDefaultRole,
        boolean ldapUseSsl,
        boolean ldapConfigured,
        boolean maintenanceEnabled,
        String maintenanceMessage,
        // ClamAV virus scanner
        boolean clamavEnabled,
        String clamavHost,
        int clamavPort,
        // Jitsi video conferencing (enabled via modules map)
        String jitsiServerUrl,
        // WOPI / ONLYOFFICE
        boolean wopiEnabled,
        String wopiOfficeUrl
) {
}
