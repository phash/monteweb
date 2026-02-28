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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements AuthModuleApi {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserModuleApi userModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;

    @Autowired(required = false)
    private LdapAuthService ldapAuthService;

    public AuthService(UserModuleApi userModuleApi,
                       AdminModuleApi adminModuleApi,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder,
                       TotpService totpService) {
        this.userModuleApi = userModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
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
        // First try local DB authentication
        UserInfo user = null;
        boolean localAuthSuccess = false;

        var existingUser = userModuleApi.findByEmail(request.email());
        if (existingUser.isPresent()) {
            user = existingUser.get();
            var storedHash = userModuleApi.getPasswordHash(request.email());
            if (storedHash.isPresent() && passwordEncoder.matches(request.password(), storedHash.get())) {
                localAuthSuccess = true;
            }
        }

        // If local auth failed, try LDAP
        if (!localAuthSuccess && ldapAuthService != null && ldapAuthService.isLdapEnabled()) {
            log.debug("Local auth failed for {}, attempting LDAP authentication", request.email());
            var ldapUser = ldapAuthService.authenticate(request.email(), request.password());
            if (ldapUser != null) {
                log.info("LDAP authentication successful for {}", ldapUser.email());
                user = findOrCreateLdapUser(ldapUser);
                localAuthSuccess = true;
            }
        }

        if (!localAuthSuccess || user == null) {
            throw new BusinessException("Invalid credentials");
        }

        if (!user.active()) {
            throw new BusinessException("PENDING_APPROVAL");
        }

        // Check if user has 2FA enabled
        if (userModuleApi.isTotpEnabled(user.id())) {
            String tempToken = jwtService.generateTempToken(user.id(), user.email(), user.role().name());
            return LoginResponse.twoFactorChallenge(tempToken);
        }

        // Check if 2FA is MANDATORY and grace period passed
        var tenantConfig = adminModuleApi.getTenantConfig();
        if ("MANDATORY".equals(tenantConfig.twoFactorMode())) {
            if (tenantConfig.twoFactorGraceDeadline() != null
                    && java.time.Instant.now().isAfter(tenantConfig.twoFactorGraceDeadline())) {
                // Grace period passed, user must set up 2FA before proceeding
                String tempToken = jwtService.generateTempToken(user.id(), user.email(), user.role().name());
                return LoginResponse.twoFactorSetupRequired(tempToken);
            }
        }

        userModuleApi.updateLastLogin(user.id());
        return generateTokenResponse(user);
    }

    /**
     * Finds an existing user by email or creates a new one from LDAP attributes.
     * If the user already exists, updates their attributes from LDAP.
     */
    private UserInfo findOrCreateLdapUser(LdapAuthService.LdapUserInfo ldapUser) {
        var existing = userModuleApi.findByEmail(ldapUser.email());
        if (existing.isPresent()) {
            // Update existing user attributes from LDAP on each login
            var user = existing.get();
            userModuleApi.updateProfile(user.id(), ldapUser.firstName(), ldapUser.lastName(), null);
            // Re-fetch after update
            return userModuleApi.findById(user.id()).orElse(user);
        }

        // Auto-create user from LDAP (no password — can't do local login)
        UserRole role = UserRole.fromStringOrNull(ldapUser.defaultRole());
        if (role == null) {
            role = UserRole.PARENT;
        }
        UserInfo newUser = userModuleApi.createUser(
                ldapUser.email(),
                null, // no password hash — LDAP-only user
                ldapUser.firstName() != null ? ldapUser.firstName() : "",
                ldapUser.lastName() != null ? ldapUser.lastName() : "",
                null, // no phone
                role
        );
        // LDAP-created users are immediately active (skip approval)
        userModuleApi.setActive(newUser.id(), true);
        return userModuleApi.findById(newUser.id()).orElse(newUser);
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

    /**
     * 2FA Setup: Generate a new TOTP secret and return the QR URI.
     */
    public TwoFactorSetupResponse setup2fa(java.util.UUID userId) {
        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        String secret = totpService.generateSecret();
        userModuleApi.setTotpSecret(userId, secret);

        String qrUri = totpService.generateTotpUri(secret, user.email());
        return new TwoFactorSetupResponse(secret, qrUri);
    }

    /**
     * 2FA Confirm: Verify code against stored secret, enable 2FA, return recovery codes.
     */
    public TwoFactorConfirmResponse confirm2fa(java.util.UUID userId, String code) {
        String secret = userModuleApi.getTotpSecret(userId)
                .orElseThrow(() -> new BusinessException("2FA not set up. Call setup first."));

        if (!totpService.verifyCode(secret, code)) {
            throw new BusinessException("Invalid 2FA code");
        }

        var recoveryCodes = totpService.generateRecoveryCodes();
        var hashedCodes = recoveryCodes.stream()
                .map(totpService::hashRecoveryCode)
                .toArray(String[]::new);
        userModuleApi.enableTotp(userId, hashedCodes);

        return new TwoFactorConfirmResponse(recoveryCodes); // return plaintext to user once
    }

    /**
     * 2FA Disable: Verify password and disable 2FA for the user.
     */
    public void disable2fa(java.util.UUID userId, String password) {
        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        String storedHash = userModuleApi.getPasswordHash(user.email())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordEncoder.matches(password, storedHash)) {
            throw new BusinessException("Invalid password");
        }

        userModuleApi.disableTotp(userId);
    }

    /**
     * 2FA Verify: Validate temp token + TOTP code, return real tokens.
     * Also accepts recovery codes (consumed on use).
     */
    public LoginResponse verify2fa(String tempToken, String code) {
        var claimsOpt = jwtService.validateTempToken(tempToken);
        if (claimsOpt.isEmpty()) {
            throw new BusinessException("Invalid or expired temp token");
        }

        var claims = claimsOpt.get();
        java.util.UUID userId = java.util.UUID.fromString(claims.getSubject());

        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Try TOTP code first
        String secret = userModuleApi.getTotpSecret(userId).orElse(null);
        if (secret != null && totpService.verifyCode(secret, code)) {
            userModuleApi.updateLastLogin(userId);
            return generateTokenResponse(user);
        }

        // Try recovery code
        String[] recoveryCodes = userModuleApi.getTotpRecoveryCodes(userId);
        if (recoveryCodes != null) {
            for (int i = 0; i < recoveryCodes.length; i++) {
                if (recoveryCodes[i] != null && totpService.verifyRecoveryCode(code, recoveryCodes[i])) {
                    // Consume recovery code
                    recoveryCodes[i] = null;
                    // Filter out nulls
                    var remaining = java.util.Arrays.stream(recoveryCodes)
                            .filter(c -> c != null)
                            .toArray(String[]::new);
                    userModuleApi.setTotpRecoveryCodes(userId, remaining);
                    userModuleApi.updateLastLogin(userId);
                    return generateTokenResponse(user);
                }
            }
        }

        throw new BusinessException("Invalid 2FA code");
    }

    /**
     * Returns whether TOTP is enabled for the given user.
     */
    public boolean is2faEnabled(java.util.UUID userId) {
        return userModuleApi.isTotpEnabled(userId);
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
