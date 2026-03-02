package com.monteweb.user.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.user.internal.model.ConsentRecord;
import com.monteweb.user.internal.model.TermsAcceptance;
import com.monteweb.user.internal.repository.ConsentRecordRepository;
import com.monteweb.user.internal.repository.TermsAcceptanceRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class PrivacyService {

    private final AdminModuleApi adminModuleApi;
    private final FamilyModuleApi familyModuleApi;
    private final ConsentRecordRepository consentRepository;
    private final TermsAcceptanceRepository termsRepository;

    public PrivacyService(AdminModuleApi adminModuleApi,
                          FamilyModuleApi familyModuleApi,
                          ConsentRecordRepository consentRepository,
                          TermsAcceptanceRepository termsRepository) {
        this.adminModuleApi = adminModuleApi;
        this.familyModuleApi = familyModuleApi;
        this.consentRepository = consentRepository;
        this.termsRepository = termsRepository;
    }

    /**
     * Get privacy policy text and version with placeholder replacement.
     */
    public Map<String, Object> getPrivacyPolicy() {
        var config = adminModuleApi.getTenantConfig();
        String text = replacePlaceholders(config.privacyPolicyText(), config);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", text);
        result.put("version", config.privacyPolicyVersion());
        return result;
    }

    /**
     * Get terms of service text and version with placeholder replacement.
     */
    public Map<String, Object> getTerms() {
        var config = adminModuleApi.getTenantConfig();
        String text = replacePlaceholders(config.termsText(), config);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", text);
        result.put("version", config.termsVersion());
        return result;
    }

    /**
     * Check if current terms version is accepted by the user.
     */
    public Map<String, Object> getTermsStatus(UUID userId) {
        var config = adminModuleApi.getTenantConfig();
        String currentVersion = config.termsVersion();
        boolean accepted = currentVersion != null &&
                termsRepository.existsByUserIdAndTermsVersion(userId, currentVersion);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentVersion", currentVersion);
        result.put("accepted", accepted);
        return result;
    }

    /**
     * Accept current terms version.
     *
     * @return a message describing the outcome
     */
    @Transactional
    public String acceptTerms(UUID userId, String ipAddress) {
        var config = adminModuleApi.getTenantConfig();
        String currentVersion = config.termsVersion();
        if (currentVersion == null) {
            return "No terms configured";
        }
        if (!termsRepository.existsByUserIdAndTermsVersion(userId, currentVersion)) {
            var acceptance = new TermsAcceptance();
            acceptance.setUserId(userId);
            acceptance.setTermsVersion(currentVersion);
            acceptance.setAcceptedAt(Instant.now());
            if (ipAddress != null) {
                acceptance.setIpAddress(ipAddress);
            }
            termsRepository.save(acceptance);
        }
        return "Terms accepted";
    }

    /**
     * Get current user's active consent records.
     */
    public List<Map<String, Object>> getConsents(UUID userId) {
        var consents = consentRepository.findByUserIdAndRevokedAtIsNull(userId);
        return consents.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("consentType", c.getConsentType());
            m.put("granted", c.isGranted());
            m.put("grantedAt", c.getGrantedAt());
            m.put("notes", c.getNotes());
            return m;
        }).toList();
    }

    /**
     * Update consent (grant or revoke). Includes IDOR check and student restriction.
     */
    @Transactional
    public void updateConsent(UUID callerUserId, String consentType, Boolean granted,
                              UUID targetUserId, String notes) {
        if (targetUserId == null) {
            targetUserId = callerUserId;
        }

        // C-02: IDOR check — if setting consent for another user, verify family relationship or SUPERADMIN
        if (!targetUserId.equals(callerUserId)) {
            boolean isSuperAdmin = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
            if (!isSuperAdmin) {
                var callerFamilies = familyModuleApi.findByUserId(callerUserId);
                var targetFamilies = familyModuleApi.findByUserId(targetUserId);
                boolean inSameFamily = callerFamilies.stream()
                        .anyMatch(cf -> targetFamilies.stream().anyMatch(tf -> tf.id().equals(cf.id())));
                if (!inSameFamily) {
                    throw new ForbiddenException("You can only update consent for members of your own family");
                }
            }
        }

        // Block students from setting their own consent — requires parent/guardian
        if (targetUserId.equals(callerUserId)) {
            boolean isStudent = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
            if (isStudent) {
                throw new ForbiddenException(
                        "Students cannot update their own consent. This must be done by a parent or guardian.");
            }
        }

        // Revoke existing active consent of this type if it exists
        var existing = consentRepository.findByUserIdAndConsentTypeAndRevokedAtIsNull(targetUserId, consentType);
        existing.ifPresent(c -> {
            c.setRevokedAt(Instant.now());
            consentRepository.save(c);
        });

        // Create new consent record
        var record = new ConsentRecord();
        record.setUserId(targetUserId);
        record.setGrantedBy(callerUserId);
        record.setConsentType(consentType);
        record.setGranted(granted);
        record.setGrantedAt(Instant.now());
        record.setNotes(notes);
        consentRepository.save(record);
    }

    private String replacePlaceholders(String text, TenantConfigInfo config) {
        if (text == null) return null;
        String schoolName = config.schoolFullName() != null ? config.schoolFullName() : config.schoolName();
        String address = config.schoolAddress() != null ? config.schoolAddress() : "";
        String principal = config.schoolPrincipal() != null ? config.schoolPrincipal() : "";
        String techName = config.techContactName() != null ? config.techContactName() : "";
        String techEmail = config.techContactEmail() != null ? config.techContactEmail() : "";
        return text
                .replace("{{SCHOOL_NAME}}", schoolName)
                .replace("{{SCHOOL_ADDRESS}}", address)
                .replace("{{SCHOOL_PRINCIPAL}}", principal)
                .replace("{{TECH_CONTACT_NAME}}", techName)
                .replace("{{TECH_CONTACT_EMAIL}}", techEmail);
    }
}
