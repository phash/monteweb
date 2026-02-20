package com.monteweb.auth.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.auth.AuthModuleApi;
import com.monteweb.auth.TokenClaims;
import com.monteweb.auth.TokenResponse;
import com.monteweb.auth.internal.dto.*;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements AuthModuleApi {

    private final UserModuleApi userModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserModuleApi userModuleApi,
                       AdminModuleApi adminModuleApi,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder) {
        this.userModuleApi = userModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse register(RegisterRequest request) {
        if (userModuleApi.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        UserInfo user = userModuleApi.createUser(
                request.email(),
                passwordHash,
                request.firstName(),
                request.lastName(),
                request.phone(),
                UserRole.PARENT
        );

        // Deactivate new users only if admin requires approval
        boolean requireApproval = adminModuleApi.getTenantConfig().requireUserApproval();
        if (requireApproval) {
            userModuleApi.setActive(user.id(), false);
            return null;
        }

        return generateTokenResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        UserInfo user = userModuleApi.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        String storedHash = userModuleApi.getPasswordHash(request.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), storedHash)) {
            throw new BusinessException("Invalid credentials");
        }

        if (!user.active()) {
            throw new BusinessException("PENDING_APPROVAL");
        }

        userModuleApi.updateLastLogin(user.id());
        return generateTokenResponse(user);
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        var userId = refreshTokenService.validateAndRotate(request.refreshToken());
        if (userId == null) {
            throw new BusinessException("Invalid or expired refresh token");
        }

        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        return generateTokenResponse(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    @Override
    public TokenResponse generateTokensForUser(UserInfo user) {
        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.role().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.id());
        return new TokenResponse(accessToken, refreshToken, user.id(), user.email(), user.role().name());
    }

    @Override
    public java.util.Optional<TokenClaims> validateAndExtractClaims(String token) {
        if (token == null || !jwtService.validateToken(token)) {
            return java.util.Optional.empty();
        }
        var claims = jwtService.extractClaims(token);
        return java.util.Optional.of(new TokenClaims(
                claims.getSubject(),
                claims.get("role", String.class)
        ));
    }

    @Override
    public String generateImageToken(java.util.UUID userId) {
        return jwtService.generateImageToken(userId);
    }

    @Override
    public java.util.Optional<String> validateImageToken(String token) {
        return jwtService.validateImageToken(token);
    }

    private LoginResponse generateTokenResponse(UserInfo user) {
        String accessToken = jwtService.generateAccessToken(user.id(), user.email(), user.role().name());
        String refreshToken = refreshTokenService.createRefreshToken(user.id());

        return new LoginResponse(
                accessToken,
                refreshToken,
                user.id(),
                user.email(),
                user.role().name()
        );
    }
}
