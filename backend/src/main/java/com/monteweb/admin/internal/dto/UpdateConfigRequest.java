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
        List<Map<String, String>> schoolVacations,
        Boolean requireAssignmentConfirmation,
        Boolean multilanguageEnabled,
        String defaultLanguage,
        List<String> availableLanguages,
        Boolean requireUserApproval,
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
        Boolean directoryAdminOnly,
        // LDAP/AD fields
        Boolean ldapEnabled,
        String ldapUrl,
        String ldapBaseDn,
        String ldapBindDn,
        String ldapBindPassword,
        String ldapUserSearchFilter,
        String ldapAttrEmail,
        String ldapAttrFirstName,
        String ldapAttrLastName,
        String ldapDefaultRole,
        Boolean ldapUseSsl
) {
}
