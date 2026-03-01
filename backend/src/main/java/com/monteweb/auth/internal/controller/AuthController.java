package com.monteweb.auth.internal.controller;

import com.monteweb.auth.internal.dto.*;
import com.monteweb.auth.internal.service.AuthService;
import com.monteweb.auth.internal.service.PasswordResetService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody RegisterRequest request) {
        var loginResponse = authService.register(request);
        if (loginResponse != null) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(loginResponse));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "PENDING_APPROVAL"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest,
                                                            HttpServletResponse httpResponse) {
        var response = authService.login(request);
        if (response.accessToken() != null && response.refreshToken() != null) {
            setAuthCookies(httpResponse, response.accessToken(), response.refreshToken(), httpRequest);
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = cookieRefreshToken != null ? cookieRefreshToken
                : (request != null ? request.refreshToken() : null);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No refresh token provided"));
        }
        var response = authService.refresh(new RefreshTokenRequest(refreshToken));
        setAuthCookies(httpResponse, response.accessToken(), response.refreshToken(), httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = "refresh_token", required = false) String cookieRefreshToken,
            HttpServletResponse httpResponse) {
        String refreshToken = cookieRefreshToken != null ? cookieRefreshToken
                : (request != null ? request.refreshToken() : null);
        authService.logout(refreshToken);
        clearAuthCookies(httpResponse);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out"));
    }

    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(ApiResponse.ok(null, "If the email exists, a reset link was sent"));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.ok(null, "Password has been reset"));
    }

    // --- 2FA Endpoints ---

    @PostMapping("/2fa/setup")
    public ResponseEntity<ApiResponse<TwoFactorSetupResponse>> setup2fa() {
        var userId = SecurityUtils.requireCurrentUserId();
        var response = authService.setup2fa(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<ApiResponse<TwoFactorConfirmResponse>> confirm2fa(@Valid @RequestBody TwoFactorConfirmRequest request) {
        var userId = SecurityUtils.requireCurrentUserId();
        var response = authService.confirm2fa(userId, request.code());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<ApiResponse<Void>> disable2fa(@Valid @RequestBody TwoFactorDisableRequest request) {
        var userId = SecurityUtils.requireCurrentUserId();
        authService.disable2fa(userId, request.password());
        return ResponseEntity.ok(ApiResponse.ok(null, "2FA disabled"));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verify2fa(@Valid @RequestBody TwoFactorVerifyRequest request,
                                                                 HttpServletRequest httpRequest,
                                                                 HttpServletResponse httpResponse) {
        var response = authService.verify2fa(request.tempToken(), request.code());
        if (response.accessToken() != null && response.refreshToken() != null) {
            setAuthCookies(httpResponse, response.accessToken(), response.refreshToken(), httpRequest);
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/2fa/status")
    public ResponseEntity<ApiResponse<java.util.Map<String, Boolean>>> get2faStatus() {
        var userId = SecurityUtils.requireCurrentUserId();
        boolean enabled = authService.is2faEnabled(userId);
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("enabled", enabled)));
    }

    // --- Cookie helpers ---

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken,
                                 HttpServletRequest request) {
        boolean secure = isSecureRequest(request);
        String sameSite = secure ? "Strict" : "Lax";

        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofMinutes(15))
                .sameSite(sameSite)
                .secure(secure)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite(sameSite)
                .secure(secure)
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Strict")
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ZERO)
                .sameSite("Strict")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) return true;
        String forwarded = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwarded);
    }
}
