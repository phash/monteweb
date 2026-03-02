package com.monteweb.user.internal.controller;

import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.internal.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/privacy")
public class PrivacyController {

    private final PrivacyService privacyService;

    public PrivacyController(PrivacyService privacyService) {
        this.privacyService = privacyService;
    }

    /**
     * Public: Get privacy policy text and version (with placeholder replacement).
     */
    @GetMapping("/policy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrivacyPolicy() {
        return ResponseEntity.ok(ApiResponse.ok(privacyService.getPrivacyPolicy()));
    }

    /**
     * Public: Get terms of service text and version (with placeholder replacement).
     */
    @GetMapping("/terms")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTerms() {
        return ResponseEntity.ok(ApiResponse.ok(privacyService.getTerms()));
    }

    /**
     * Authenticated: Check if current terms are accepted.
     */
    @GetMapping("/terms/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTermsStatus() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(privacyService.getTermsStatus(userId)));
    }

    /**
     * Authenticated: Accept current terms version.
     */
    @PostMapping("/terms/accept")
    public ResponseEntity<ApiResponse<Void>> acceptTerms(@RequestBody(required = false) Map<String, String> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String ipAddress = body != null ? body.get("ipAddress") : null;
        String message = privacyService.acceptTerms(userId, ipAddress);
        return ResponseEntity.ok(ApiResponse.ok(null, message));
    }

    /**
     * Authenticated: Get current user's consent records.
     */
    @GetMapping("/consents")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConsents() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(privacyService.getConsents(userId)));
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

        privacyService.updateConsent(userId, consentType, granted, targetUserId, notes);
        return ResponseEntity.ok(ApiResponse.ok(null, "Consent updated"));
    }
}
