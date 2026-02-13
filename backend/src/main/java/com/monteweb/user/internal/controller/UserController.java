package com.monteweb.user.internal.controller;

import com.monteweb.auth.AuthModuleApi;
import com.monteweb.auth.TokenResponse;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.AvatarUtils;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.internal.dto.SwitchRoleRequest;
import com.monteweb.user.internal.dto.UpdateProfileRequest;
import com.monteweb.user.internal.service.UserService;
import com.monteweb.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthModuleApi authModuleApi;

    public UserController(UserService userService, AuthModuleApi authModuleApi) {
        this.userService = userService;
        this.authModuleApi = authModuleApi;
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

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserInfo>>> searchUsers(
            @RequestParam(defaultValue = "") String q,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = userService.searchUsers(q, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserInfo>> getUserById(@PathVariable UUID id) {
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
     * DSGVO: Delete / anonymize the current user's account.
     */
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@RequestBody(required = false) Map<String, String> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String reason = body != null ? body.get("reason") : null;
        userService.anonymizeUser(userId, reason);
        return ResponseEntity.ok(ApiResponse.ok(null, "Account has been deleted"));
    }
}
