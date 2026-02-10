package com.monteweb.user.internal.controller;

import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.internal.dto.UpdateProfileRequest;
import com.monteweb.user.internal.service.UserService;
import com.monteweb.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> getCurrentUser() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserInfo>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userService.updateProfile(userId, request.firstName(), request.lastName(), request.phone());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserInfo>> getUserById(@PathVariable UUID id) {
        var user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
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
