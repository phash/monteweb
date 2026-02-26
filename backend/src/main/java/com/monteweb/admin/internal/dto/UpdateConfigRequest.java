package com.monteweb.admin.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateConfigRequest(
        @Size(max = 255) String schoolName,
        @Size(max = 1000) String logoUrl,
        @DecimalMin("0") BigDecimal targetHoursPerFamily,
        @DecimalMin("0") BigDecimal targetCleaningHours,
        @Size(max = 5) String bundesland,
        List<Map<String, String>> schoolVacations,
        Boolean requireAssignmentConfirmation,
        Boolean multilanguageEnabled,
        @Size(max = 10) String defaultLanguage,
        List<String> availableLanguages,
        Boolean requireUserApproval,
        @Size(max = 100000) String privacyPolicyText,
        @Size(max = 50) String privacyPolicyVersion,
        @Size(max = 100000) String termsText,
        @Size(max = 50) String termsVersion,
        @Min(1) @Max(3650) Integer dataRetentionDaysNotifications,
        @Min(1) @Max(3650) Integer dataRetentionDaysAudit,
        @Size(max = 255) String schoolFullName,
        @Size(max = 500) String schoolAddress,
        @Size(max = 255) String schoolPrincipal,
        @Size(max = 255) String techContactName,
        @Size(max = 255) String techContactEmail,
        @Size(max = 20) String twoFactorMode,
        // directoryAdminOnly enabled via modules map
        // LDAP/AD fields (enabled via modules map)
        @Size(max = 500) String ldapUrl,
        @Size(max = 500) String ldapBaseDn,
        @Size(max = 500) String ldapBindDn,
        @Size(max = 500) String ldapBindPassword,
        @Size(max = 500) String ldapUserSearchFilter,
        @Size(max = 255) String ldapAttrEmail,
        @Size(max = 255) String ldapAttrFirstName,
        @Size(max = 255) String ldapAttrLastName,
        @Size(max = 50) String ldapDefaultRole,
        Boolean ldapUseSsl,
        // ClamAV virus scanner (enabled via modules map)
        @Size(max = 255) String clamavHost,
        @Min(1) @Max(65535) Integer clamavPort,
        // Jitsi video conferencing (enabled via modules map)
        @Size(max = 500) String jitsiServerUrl,
        // WOPI / ONLYOFFICE (enabled via modules map)
        @Size(max = 500) String wopiOfficeUrl,
        // Communication rules
        Boolean parentToParentMessaging,
        Boolean studentToStudentMessaging,
        // Family settings
        Boolean soleCustodyEnabled,
        Boolean requireFamilySwitchApproval
) {
}
