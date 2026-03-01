package com.monteweb.user.internal.controller;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.auth.AuthModuleApi;
import com.monteweb.auth.TokenResponse;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.AvatarUtils;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserRole;
import com.monteweb.user.internal.dto.SwitchRoleRequest;
import com.monteweb.user.internal.dto.UpdateDigestRequest;
import com.monteweb.user.internal.dto.UpdateProfileRequest;
import com.monteweb.user.internal.service.UserService;
import com.monteweb.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthModuleApi authModuleApi;
    private final AdminModuleApi adminModuleApi;

    public UserController(UserService userService, AuthModuleApi authModuleApi, AdminModuleApi adminModuleApi) {
        this.userService = userService;
        this.authModuleApi = authModuleApi;
        this.adminModuleApi = adminModuleApi;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/me/active-role")
    public ResponseEntity<ApiResponse<TokenResponse>> switchActiveRole(@Valid @RequestBody SwitchRoleRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var updatedUser = userService.switchActiveRole(userId, request.role());
        var tokenResponse = authModuleApi.generateTokensForUser(updatedUser);
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userService.updateProfile(userId, request.firstName(), request.lastName(), request.phone());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/me/digest")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDigestPreference() {
        var userId = SecurityUtils.requireCurrentUserId();
        var freq = userService.getDigestFrequency(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("frequency", freq)));
    }

    @PutMapping("/me/digest")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateDigestPreference(
            @Valid @RequestBody UpdateDigestRequest request) {
        var userId = SecurityUtils.requireCurrentUserId();
        userService.updateDigestFrequency(userId, request.frequency());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("frequency", request.frequency())));
    }

    @GetMapping("/me/dark-mode")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDarkMode() {
        var userId = SecurityUtils.requireCurrentUserId();
        var mode = userService.getDarkMode(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("darkMode", mode)));
    }

    @PutMapping("/me/dark-mode")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateDarkMode(@RequestBody Map<String, String> body) {
        var userId = SecurityUtils.requireCurrentUserId();
        var mode = body.getOrDefault("darkMode", "SYSTEM");
        if (!Set.of("SYSTEM", "LIGHT", "DARK").contains(mode)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid dark mode: must be SYSTEM, LIGHT, or DARK"));
        }
        userService.updateDarkMode(userId, mode);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("darkMode", mode)));
    }

    @GetMapping("/directory")
    public ResponseEntity<ApiResponse<PageResponse<UserInfo>>> getDirectory(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false, defaultValue = "") String q,
            @PageableDefault(size = 24, sort = "lastName") Pageable pageable) {
        if (adminModuleApi.isModuleEnabled("directoryAdminOnly") && SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"))) {
            return ResponseEntity.status(403).body(ApiResponse.error("Directory access restricted to admins"));
        }
        var page = userService.findDirectory(UserRole.fromStringOrNull(role), sectionId, roomId, q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserInfo>>> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserInfo>> getUserById(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.requireCurrentUserId();
        // Self-access always allowed; TEACHER+ always allowed.
        // Others (PARENT, STUDENT) only when directory is not restricted to admins.
        if (!id.equals(currentUserId)) {
            boolean isElevated = SecurityContextHolder.getContext().getAuthentication()
                    .getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN")
                            || a.getAuthority().equals("ROLE_SECTION_ADMIN")
                            || a.getAuthority().equals("ROLE_TEACHER"));
            if (!isElevated && adminModuleApi.isModuleEnabled("directoryAdminOnly")) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
        }
        var user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserInfo>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String dataUrl = AvatarUtils.validateAndConvert(file);
        var user = userService.updateAvatarUrl(userId, dataUrl);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<ApiResponse<UserInfo>> removeAvatar() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userService.updateAvatarUrl(userId, null);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    /**
     * DSGVO: Export all personal data for the current user.
     */
    @GetMapping("/me/data-export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportUserData() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var data = userService.exportUserData(userId);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /**
     * DSGVO: Request account deletion with 14-day grace period.
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        userService.requestDeletion(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deletion requested. Your account will be deleted in 14 days."));
    }

    /**
     * DSGVO: Cancel a pending account deletion.
     */
    @PostMapping("/me/cancel-deletion")
    public ResponseEntity<ApiResponse<Void>> cancelDeletion() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        userService.cancelDeletion(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deletion cancelled"));
    }

    /**
     * DSGVO: Get current deletion status.
     */
    @GetMapping("/me/deletion-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeletionStatus() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userService.findEntityById(userId);
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("deletionRequested", user.getDeletionRequestedAt() != null);
        status.put("deletionRequestedAt", user.getDeletionRequestedAt());
        status.put("scheduledDeletionAt", user.getScheduledDeletionAt());
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
