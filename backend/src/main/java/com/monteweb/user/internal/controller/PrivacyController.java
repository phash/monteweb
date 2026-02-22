package com.monteweb.user.internal.controller;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.internal.model.ConsentRecord;
import com.monteweb.user.internal.model.TermsAcceptance;
import com.monteweb.user.internal.repository.ConsentRecordRepository;
import com.monteweb.user.internal.repository.TermsAcceptanceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private final AdminModuleApi adminModuleApi;
    private final ConsentRecordRepository consentRepository;
    private final TermsAcceptanceRepository termsRepository;

    public PrivacyController(AdminModuleApi adminModuleApi,
                              ConsentRecordRepository consentRepository,
                              TermsAcceptanceRepository termsRepository) {
        this.adminModuleApi = adminModuleApi;
        this.consentRepository = consentRepository;
        this.termsRepository = termsRepository;
    }

    /**
     * Public: Get privacy policy text and version.
     */
    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrivacyPolicy() {
        var config = adminModuleApi.getTenantConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", config.privacyPolicyText());
        result.put("version", config.privacyPolicyVersion());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Public: Get terms of service text and version.
     */
    @GetMapping("/terms")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTerms() {
        var config = adminModuleApi.getTenantConfig();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", config.termsText());
        result.put("version", config.termsVersion());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Authenticated: Check if current terms are accepted.
     */
    @GetMapping("/terms/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTermsStatus() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var config = adminModuleApi.getTenantConfig();
        String currentVersion = config.termsVersion();
        boolean accepted = currentVersion != null &&
                termsRepository.existsByUserIdAndTermsVersion(userId, currentVersion);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentVersion", currentVersion);
        result.put("accepted", accepted);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Authenticated: Accept current terms version.
     */
    @PostMapping("/terms/accept")
    public ResponseEntity<ApiResponse<Void>> acceptTerms(@RequestBody(required = false) Map<String, String> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var config = adminModuleApi.getTenantConfig();
        String currentVersion = config.termsVersion();
        if (currentVersion == null) {
            return ResponseEntity.ok(ApiResponse.ok(null, "No terms configured"));
        }
        if (!termsRepository.existsByUserIdAndTermsVersion(userId, currentVersion)) {
            var acceptance = new TermsAcceptance();
            acceptance.setUserId(userId);
            acceptance.setTermsVersion(currentVersion);
            acceptance.setAcceptedAt(Instant.now());
            if (body != null && body.containsKey("ipAddress")) {
                acceptance.setIpAddress(body.get("ipAddress"));
            }
            termsRepository.save(acceptance);
        }
        return ResponseEntity.ok(ApiResponse.ok(null, "Terms accepted"));
    }

    /**
     * Authenticated: Get current user's consent records.
     */
    @GetMapping("/consents")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConsents() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var consents = consentRepository.findByUserIdAndRevokedAtIsNull(userId);
        var result = consents.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("consentType", c.getConsentType());
            m.put("granted", c.isGranted());
            m.put("grantedAt", c.getGrantedAt());
            m.put("notes", c.getNotes());
            return m;
        }).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Authenticated: Update consent (grant or revoke).
     */
    @PutMapping("/consents")
    public ResponseEntity<ApiResponse<Void>> updateConsent(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String consentType = (String) body.get("consentType");
        Boolean granted = (Boolean) body.get("granted");
        UUID targetUserId = body.containsKey("targetUserId")
                ? UUID.fromString((String) body.get("targetUserId")) : userId;
        String notes = (String) body.get("notes");

        if (consentType == null || granted == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("consentType and granted are required"));
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
        record.setGrantedBy(userId);
        record.setConsentType(consentType);
        record.setGranted(granted);
        record.setGrantedAt(Instant.now());
        record.setNotes(notes);
        consentRepository.save(record);

        return ResponseEntity.ok(ApiResponse.ok(null, "Consent updated"));
    }
}
