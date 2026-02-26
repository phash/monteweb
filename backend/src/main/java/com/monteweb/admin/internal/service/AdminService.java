package com.monteweb.admin.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.admin.internal.model.TenantConfig;
import com.monteweb.admin.internal.repository.TenantConfigRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminService implements AdminModuleApi {

    private final TenantConfigRepository configRepository;

    public AdminService(TenantConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    public TenantConfigInfo getTenantConfig() {
        return toInfo(getConfig());
    }

    @Override
    public boolean isModuleEnabled(String moduleName) {
        var config = getConfig();
        return config.getModules().getOrDefault(moduleName, false);
    }

    @Transactional
    public TenantConfigInfo updateConfig(String schoolName, String logoUrl, BigDecimal targetHours, BigDecimal targetCleaningHours,
                                          String bundesland, java.util.List<java.util.Map<String, String>> schoolVacations,
                                          Boolean requireAssignmentConfirmation, Boolean multilanguageEnabled,
                                          String defaultLanguage, java.util.List<String> availableLanguages,
                                          Boolean requireUserApproval,
                                          String privacyPolicyText, String privacyPolicyVersion,
                                          String termsText, String termsVersion,
                                          Integer dataRetentionDaysNotifications, Integer dataRetentionDaysAudit,
                                          String schoolFullName, String schoolAddress, String schoolPrincipal,
                                          String techContactName, String techContactEmail,
                                          String twoFactorMode,
                                          String ldapUrl, String ldapBaseDn,
                                          String ldapBindDn, String ldapBindPassword,
                                          String ldapUserSearchFilter, String ldapAttrEmail,
                                          String ldapAttrFirstName, String ldapAttrLastName,
                                          String ldapDefaultRole, Boolean ldapUseSsl,
                                          String clamavHost, Integer clamavPort,
                                          String jitsiServerUrl,
                                          String wopiOfficeUrl,
                                          Boolean parentToParentMessaging,
                                          Boolean studentToStudentMessaging,
                                          Boolean soleCustodyEnabled,
                                          Boolean requireFamilySwitchApproval) {
        var config = getConfig();
        if (schoolName != null) config.setSchoolName(schoolName);
        if (logoUrl != null) config.setLogoUrl(logoUrl);
        if (targetHours != null) config.setTargetHoursPerFamily(targetHours);
        if (targetCleaningHours != null) config.setTargetCleaningHours(targetCleaningHours);
        if (bundesland != null) config.setBundesland(bundesland);
        if (schoolVacations != null) config.setSchoolVacations(schoolVacations);
        if (requireAssignmentConfirmation != null) config.setRequireAssignmentConfirmation(requireAssignmentConfirmation);
        if (multilanguageEnabled != null) config.setMultilanguageEnabled(multilanguageEnabled);
        if (defaultLanguage != null) config.setDefaultLanguage(defaultLanguage);
        if (availableLanguages != null) config.setAvailableLanguages(availableLanguages);
        if (requireUserApproval != null) config.setRequireUserApproval(requireUserApproval);
        if (privacyPolicyText != null) config.setPrivacyPolicyText(privacyPolicyText);
        if (privacyPolicyVersion != null) config.setPrivacyPolicyVersion(privacyPolicyVersion);
        if (termsText != null) config.setTermsText(termsText);
        if (termsVersion != null) config.setTermsVersion(termsVersion);
        if (dataRetentionDaysNotifications != null) config.setDataRetentionDaysNotifications(dataRetentionDaysNotifications);
        if (dataRetentionDaysAudit != null) config.setDataRetentionDaysAudit(dataRetentionDaysAudit);
        if (schoolFullName != null) config.setSchoolFullName(schoolFullName);
        if (schoolAddress != null) config.setSchoolAddress(schoolAddress);
        if (schoolPrincipal != null) config.setSchoolPrincipal(schoolPrincipal);
        if (techContactName != null) config.setTechContactName(techContactName);
        if (techContactEmail != null) config.setTechContactEmail(techContactEmail);
        if (twoFactorMode != null) {
            String oldMode = config.getTwoFactorMode();
            config.setTwoFactorMode(twoFactorMode);
            if ("MANDATORY".equals(twoFactorMode) && !"MANDATORY".equals(oldMode)) {
                // Set 7-day grace period when switching to MANDATORY
                config.setTwoFactorGraceDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
            } else if (!"MANDATORY".equals(twoFactorMode)) {
                config.setTwoFactorGraceDeadline(null);
            }
        }
        // LDAP/AD fields (enabled via modules map)
        if (ldapUrl != null) config.setLdapUrl(ldapUrl);
        if (ldapBaseDn != null) config.setLdapBaseDn(ldapBaseDn);
        if (ldapBindDn != null) config.setLdapBindDn(ldapBindDn);
        if (ldapBindPassword != null && !ldapBindPassword.isBlank()) config.setLdapBindPassword(ldapBindPassword);
        if (ldapUserSearchFilter != null) config.setLdapUserSearchFilter(ldapUserSearchFilter);
        if (ldapAttrEmail != null) config.setLdapAttrEmail(ldapAttrEmail);
        if (ldapAttrFirstName != null) config.setLdapAttrFirstName(ldapAttrFirstName);
        if (ldapAttrLastName != null) config.setLdapAttrLastName(ldapAttrLastName);
        if (ldapDefaultRole != null) config.setLdapDefaultRole(ldapDefaultRole);
        if (ldapUseSsl != null) config.setLdapUseSsl(ldapUseSsl);
        // Maintenance mode — handled via dedicated method
        // ClamAV virus scanner (enabled via modules map)
        if (clamavHost != null) config.setClamavHost(clamavHost);
        if (clamavPort != null) config.setClamavPort(clamavPort);
        // Jitsi video conferencing (enabled via modules map)
        if (jitsiServerUrl != null) config.setJitsiServerUrl(jitsiServerUrl);
        // WOPI / ONLYOFFICE (enabled via modules map)
        if (wopiOfficeUrl != null) config.setWopiOfficeUrl(wopiOfficeUrl);
        // Communication rules
        if (parentToParentMessaging != null) config.setParentToParentMessaging(parentToParentMessaging);
        if (studentToStudentMessaging != null) config.setStudentToStudentMessaging(studentToStudentMessaging);
        // Family settings
        if (soleCustodyEnabled != null) config.setSoleCustodyEnabled(soleCustodyEnabled);
        if (requireFamilySwitchApproval != null) config.setRequireFamilySwitchApproval(requireFamilySwitchApproval);
        return toInfo(configRepository.save(config));
    }

    @Transactional
    public TenantConfigInfo updateTheme(Map<String, Object> theme) {
        var config = getConfig();
        config.setTheme(theme);
        return toInfo(configRepository.save(config));
    }

    @Transactional
    public TenantConfigInfo updateModules(Map<String, Boolean> modules) {
        var config = getConfig();
        config.setModules(modules);
        return toInfo(configRepository.save(config));
    }

    /**
     * Upload logo as base64 data URL (stored directly in tenant config).
     * This avoids a MinIO dependency for the admin module.
     * For production, consider a dedicated file service.
     */
    @Transactional
    public TenantConfigInfo uploadLogo(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("Logo file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("File must be an image");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException("Logo must be smaller than 2MB");
        }
        try {
            byte[] bytes = file.getBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + contentType + ";base64," + base64;

            var config = getConfig();
            config.setLogoUrl(dataUrl);
            return toInfo(configRepository.save(config));
        } catch (java.io.IOException e) {
            throw new BusinessException("Failed to read logo file");
        }
    }

    @Override
    public String getLdapBindPassword() {
        return getConfig().getLdapBindPassword();
    }

    @Override
    public boolean isMaintenanceEnabled() {
        return isModuleEnabled("maintenance");
    }

    @Transactional
    public TenantConfigInfo updateMaintenance(boolean enabled, String message) {
        var config = getConfig();
        // Toggle maintenance in modules map
        var modules = new java.util.HashMap<>(config.getModules());
        modules.put("maintenance", enabled);
        config.setModules(modules);
        config.setMaintenanceMessage(message);
        return toInfo(configRepository.save(config));
    }

    /**
     * Tests an LDAP connection with the given parameters.
     */
    public void testLdapConnection(String url, String baseDn, String bindDn, String bindPassword, boolean useSsl) {
        java.util.Hashtable<String, String> env = new java.util.Hashtable<>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, url);
        if (useSsl) {
            env.put(javax.naming.Context.SECURITY_PROTOCOL, "ssl");
        }
        if (bindDn != null && !bindDn.isBlank()) {
            env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, bindDn);
            env.put(javax.naming.Context.SECURITY_CREDENTIALS, bindPassword != null ? bindPassword : "");
        }
        env.put("com.sun.jndi.ldap.connect.timeout", "5000");
        env.put("com.sun.jndi.ldap.read.timeout", "5000");

        try {
            var ctx = new javax.naming.directory.InitialDirContext(env);
            ctx.close();
        } catch (javax.naming.NamingException e) {
            throw new BusinessException("LDAP connection failed: " + e.getMessage());
        }
    }

    private TenantConfig getConfig() {
        return configRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tenant configuration not found"));
    }

    private TenantConfigInfo toInfo(TenantConfig config) {
        return new TenantConfigInfo(
                config.getId(),
                config.getSchoolName(),
                config.getLogoUrl(),
                config.getTheme(),
                config.getModules(),
                config.getTargetHoursPerFamily(),
                config.getTargetCleaningHours(),
                config.isParentToParentMessaging(),
                config.isStudentToStudentMessaging(),
                config.getBundesland(),
                config.getSchoolVacations(),
                config.getGithubRepo(),
                config.getGithubPat() != null && !config.getGithubPat().isBlank(),
                config.isRequireAssignmentConfirmation(),
                config.isMultilanguageEnabled(),
                config.getDefaultLanguage(),
                config.getAvailableLanguages(),
                config.isRequireUserApproval(),
                config.getPrivacyPolicyText(),
                config.getPrivacyPolicyVersion(),
                config.getTermsText(),
                config.getTermsVersion(),
                config.getDataRetentionDaysNotifications(),
                config.getDataRetentionDaysAudit(),
                config.getSchoolFullName(),
                config.getSchoolAddress(),
                config.getSchoolPrincipal(),
                config.getTechContactName(),
                config.getTechContactEmail(),
                config.getTwoFactorMode(),
                config.getTwoFactorGraceDeadline(),
                // LDAP/AD fields (password never exposed; enabled via modules map)
                config.getLdapUrl(),
                config.getLdapBaseDn(),
                config.getLdapBindDn(),
                config.getLdapUserSearchFilter(),
                config.getLdapAttrEmail(),
                config.getLdapAttrFirstName(),
                config.getLdapAttrLastName(),
                config.getLdapDefaultRole(),
                config.isLdapUseSsl(),
                config.getLdapUrl() != null && !config.getLdapUrl().isBlank()
                        && config.getLdapBaseDn() != null && !config.getLdapBaseDn().isBlank(),
                // Maintenance (enabled via modules map)
                config.getMaintenanceMessage(),
                // ClamAV virus scanner (enabled via modules map)
                config.getClamavHost(),
                config.getClamavPort(),
                // Jitsi video conferencing (enabled via modules map)
                config.getJitsiServerUrl(),
                // WOPI / ONLYOFFICE (enabled via modules map)
                config.getWopiOfficeUrl(),
                // Family settings
                config.isSoleCustodyEnabled(),
                config.isRequireFamilySwitchApproval()
        );
    }
}
