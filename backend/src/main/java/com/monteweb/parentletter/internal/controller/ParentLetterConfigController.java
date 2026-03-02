package com.monteweb.parentletter.internal.controller;

import com.monteweb.parentletter.ParentLetterConfigInfo;
import com.monteweb.parentletter.UpdateParentLetterConfigRequest;
import com.monteweb.parentletter.internal.service.ParentLetterService;
import com.monteweb.parentletter.internal.service.ParentLetterStorageService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for parent letter configuration (letterhead, signature template, reminder days).
 * Access restricted to SUPERADMIN or SECTION_ADMIN (for their own section).
 */
@RestController
@RequestMapping("/api/v1/parent-letter-config")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterConfigController {

    private final ParentLetterService parentLetterService;
    private final ParentLetterStorageService storageService;
    private final UserModuleApi userModuleApi;

    public ParentLetterConfigController(ParentLetterService parentLetterService,
                                        ParentLetterStorageService storageService,
                                        UserModuleApi userModuleApi) {
        this.parentLetterService = parentLetterService;
        this.storageService = storageService;
        this.userModuleApi = userModuleApi;
    }

    /**
     * Get the config for a section (or global if sectionId is omitted).
     *
     * @param sectionId optional — if provided, returns the section-specific config (with global fallback)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ParentLetterConfigInfo>> getConfig(
            @RequestParam(required = false) UUID sectionId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        requireAdminAccess(userId);
        var config = parentLetterService.getConfig(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    /**
     * Update the config for a section (or global if sectionId is omitted).
     */
    @PutMapping
    public ResponseEntity<ApiResponse<ParentLetterConfigInfo>> updateConfig(
            @Valid @RequestBody UpdateParentLetterConfigRequest request,
            @RequestParam(required = false) UUID sectionId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        requireAdminAccess(userId);
        var config = parentLetterService.updateConfig(request, sectionId);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    /**
     * Upload a letterhead image for a section (or global).
     * Accepts image/jpeg, image/png, image/svg+xml, application/pdf.
     */
    @PostMapping(value = "/letterhead", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ParentLetterConfigInfo>> uploadLetterhead(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) UUID sectionId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        requireAdminAccess(userId);

        // Delete existing letterhead if present
        var existingConfig = parentLetterService.getConfig(sectionId);
        if (existingConfig.letterheadPath() != null) {
            storageService.deleteLetterhead(existingConfig.letterheadPath());
        }

        String storagePath = storageService.uploadLetterhead(sectionId, file);
        var config = parentLetterService.updateLetterheadPath(sectionId, storagePath);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    /**
     * Remove the letterhead image for a section (or global).
     */
    @DeleteMapping("/letterhead")
    public ResponseEntity<ApiResponse<ParentLetterConfigInfo>> deleteLetterhead(
            @RequestParam(required = false) UUID sectionId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        requireAdminAccess(userId);

        var existingConfig = parentLetterService.getConfig(sectionId);
        if (existingConfig.letterheadPath() != null) {
            storageService.deleteLetterhead(existingConfig.letterheadPath());
        }

        var config = parentLetterService.removeLetterhead(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    // ---- Security helper ----

    private void requireAdminAccess(UUID userId) {
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new com.monteweb.shared.exception.ResourceNotFoundException("User", userId));

        if (user.role() == UserRole.SUPERADMIN || user.role() == UserRole.SECTION_ADMIN) {
            return;
        }

        throw new ForbiddenException("Only SUPERADMIN or SECTION_ADMIN can manage parent letter configuration");
    }
}
