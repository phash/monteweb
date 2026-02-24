package com.monteweb.profilefields.internal.controller;

import com.monteweb.profilefields.ProfileFieldInfo;
import com.monteweb.profilefields.internal.dto.CreateProfileFieldRequest;
import com.monteweb.profilefields.internal.dto.UpdateProfileFieldRequest;
import com.monteweb.profilefields.internal.service.ProfileFieldsService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@ConditionalOnProperty(prefix = "monteweb.modules", name = "profilefields.enabled", havingValue = "true")
@RequiredArgsConstructor
public class ProfileFieldsController {

    private final ProfileFieldsService profileFieldsService;

    // ---- User endpoints ----

    @GetMapping("/api/v1/profile-fields")
    public ApiResponse<List<ProfileFieldInfo>> getDefinitions() {
        SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(profileFieldsService.getFieldDefinitions());
    }

    @GetMapping("/api/v1/profile-fields/me")
    public ApiResponse<Map<String, String>> getMyValues() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(profileFieldsService.getUserFieldValues(userId));
    }

    @PutMapping("/api/v1/profile-fields/me")
    public ApiResponse<Map<String, String>> updateMyValues(@RequestBody Map<String, String> values) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ApiResponse.ok(profileFieldsService.updateUserValues(userId, values));
    }

    // ---- Admin endpoints ----

    @GetMapping("/api/v1/admin/profile-fields")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ApiResponse<List<ProfileFieldInfo>> listAllDefinitions() {
        return ApiResponse.ok(profileFieldsService.getAllDefinitions());
    }

    @PostMapping("/api/v1/admin/profile-fields")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ApiResponse<ProfileFieldInfo> createDefinition(@Valid @RequestBody CreateProfileFieldRequest request) {
        return ApiResponse.ok(profileFieldsService.createDefinition(request));
    }

    @PutMapping("/api/v1/admin/profile-fields/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ApiResponse<ProfileFieldInfo> updateDefinition(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileFieldRequest request) {
        return ApiResponse.ok(profileFieldsService.updateDefinition(id, request));
    }

    @DeleteMapping("/api/v1/admin/profile-fields/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ApiResponse<Void> deleteDefinition(@PathVariable UUID id) {
        profileFieldsService.deleteDefinition(id);
        return ApiResponse.ok(null);
    }
}
