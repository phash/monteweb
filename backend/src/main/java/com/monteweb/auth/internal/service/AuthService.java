package com.monteweb.auth.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.auth.AuthModuleApi;
import com.monteweb.auth.TokenClaims;
import com.monteweb.auth.TokenResponse;
import com.monteweb.auth.internal.dto.*;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.shared.util.AesEncryptionService;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Transactional(readOnly = true)
public class AuthService implements AuthModuleApi {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserModuleApi userModuleApi;
    private final AdminModuleApi adminModuleApi;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;
    private final AesEncryptionService aesEncryptionService;
    private final StringRedisTemplate redisTemplate;

    private static final String TWO_FA_TEMP_USED_PREFIX = "2fa_temp_used:";

    @Autowired(required = false)
    private LdapAuthService ldapAuthService;

    public AuthService(UserModuleApi userModuleApi,
                       AdminModuleApi adminModuleApi,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       PasswordEncoder passwordEncoder,
                       TotpService totpService,
                       AesEncryptionService aesEncryptionService,
                       StringRedisTemplate redisTemplate) {
        this.userModuleApi = userModuleApi;
        this.adminModuleApi = adminModuleApi;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordEncoder = passwordEncoder;
        this.totpService = totpService;
        this.aesEncryptionService = aesEncryptionService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
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

    @Transactional
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

        // Inactive/pending accounts must return the SAME generic error regardless of
        // password correctness, otherwise the differing "PENDING_APPROVAL" response leaks
        // (a) which emails are real pending accounts and (b) that a guessed password is
        // correct for an inactive account (online password oracle). Check it before the
        // credential-success branch so the response is identical for right/wrong passwords.
        if (user != null && !user.active()) {
            throw new BusinessException("Invalid credentials");
        }

        if (!localAuthSuccess || user == null) {
            throw new BusinessException("Invalid credentials");
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
        // Respect requireUserApproval for LDAP-created users the same as for local registrations
        var tenantConfig = adminModuleApi.getTenantConfig();
        if (!tenantConfig.requireUserApproval()) {
            userModuleApi.setActive(newUser.id(), true);
        }
        // If approval required: leave user inactive; admin must manually approve
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

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    @Override
    @Transactional
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
        // Reject special-purpose tokens (e.g. 2fa_temp, image) that carry a "type" claim.
        // Only full access tokens may authenticate sessions (e.g. WebSocket/STOMP).
        // This mirrors the REST JwtAuthenticationFilter.authenticateWithJwt() check and
        // closes the 2FA-bypass and image-token confusion at this shared entry point.
        if (claims.get("type", String.class) != null) {
            return java.util.Optional.empty();
        }
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
    @Transactional
    public TwoFactorSetupResponse setup2fa(java.util.UUID userId) {
        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        String secret = totpService.generateSecret();
        userModuleApi.setTotpSecret(userId, aesEncryptionService.encrypt(secret));

        String qrUri = totpService.generateTotpUri(secret, user.email());
        return new TwoFactorSetupResponse(secret, qrUri);
    }

    /**
     * 2FA Confirm: Verify code against stored secret, enable 2FA, return recovery codes.
     */
    @Transactional
    public TwoFactorConfirmResponse confirm2fa(java.util.UUID userId, String code) {
        String secret = userModuleApi.getTotpSecret(userId)
                .map(aesEncryptionService::decrypt)
                .orElseThrow(() -> new BusinessException("2FA not set up. Call setup first."));

        if (!totpService.verifyCode(secret, code, userId)) {
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
    @Transactional
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
    @Transactional
    public LoginResponse verify2fa(String tempToken, String code) {
        var claimsOpt = jwtService.validateTempToken(tempToken);
        if (claimsOpt.isEmpty()) {
            throw new BusinessException("Invalid or expired temp token");
        }

        var claims = claimsOpt.get();
        java.util.UUID userId = java.util.UUID.fromString(claims.getSubject());
        String jti = claims.getId();

        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Try TOTP code first
        String secret = userModuleApi.getTotpSecret(userId).map(aesEncryptionService::decrypt).orElse(null);
        if (secret != null && totpService.verifyCode(secret, code, userId)) {
            consumeTempToken(jti, userId);
            userModuleApi.updateLastLogin(userId);
            return generateTokenResponse(user);
        }

        // Try recovery code
        String[] recoveryCodes = userModuleApi.getTotpRecoveryCodes(userId);
        if (recoveryCodes != null) {
            for (int i = 0; i < recoveryCodes.length; i++) {
                if (recoveryCodes[i] != null && totpService.verifyRecoveryCode(code, recoveryCodes[i])) {
                    consumeTempToken(jti, userId);
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
     * Self-service 2FA enrollment (step 1) during a MANDATORY-mode login after the grace
     * deadline. The login endpoint hands the client a 2FA temp token instead of a full
     * session; here we authenticate with that temp token (the user has no full session and
     * no TOTP secret yet) and generate + store a fresh TOTP secret, returning the QR URI.
     * Does NOT consume the temp token — that happens on successful {@link #confirm2faWithTempToken}.
     */
    @Transactional
    public TwoFactorSetupResponse setup2faWithTempToken(String tempToken) {
        var claims = jwtService.validateTempToken(tempToken)
                .orElseThrow(() -> new BadRequestException("Invalid or expired temp token"));
        java.util.UUID userId = java.util.UUID.fromString(claims.getSubject());

        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Users who already have 2FA enabled must use the normal verify flow, not re-enroll.
        if (userModuleApi.isTotpEnabled(userId)) {
            throw new BadRequestException("2FA is already enabled");
        }

        String secret = totpService.generateSecret();
        userModuleApi.setTotpSecret(userId, aesEncryptionService.encrypt(secret));

        String qrUri = totpService.generateTotpUri(secret, user.email());
        return new TwoFactorSetupResponse(secret, qrUri);
    }

    /**
     * Self-service 2FA enrollment (step 2) during a MANDATORY-mode login. Validates the
     * 2FA temp token + the code from the freshly configured authenticator, enables 2FA,
     * single-use-consumes the temp token, and returns a real session plus the one-time
     * recovery codes. This closes the lock-out where a user without 2FA past the grace
     * deadline could neither log in nor enroll.
     */
    @Transactional
    public TwoFactorEnrollmentResult confirm2faWithTempToken(String tempToken, String code) {
        var claims = jwtService.validateTempToken(tempToken)
                .orElseThrow(() -> new BadRequestException("Invalid or expired temp token"));
        java.util.UUID userId = java.util.UUID.fromString(claims.getSubject());
        String jti = claims.getId();

        UserInfo user = userModuleApi.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (userModuleApi.isTotpEnabled(userId)) {
            throw new BadRequestException("2FA is already enabled");
        }

        String secret = userModuleApi.getTotpSecret(userId)
                .map(aesEncryptionService::decrypt)
                .orElseThrow(() -> new BadRequestException("2FA not set up. Call setup first."));

        if (!totpService.verifyCode(secret, code, userId)) {
            throw new BusinessException("Invalid 2FA code");
        }

        var recoveryCodes = totpService.generateRecoveryCodes();
        var hashedCodes = recoveryCodes.stream()
                .map(totpService::hashRecoveryCode)
                .toArray(String[]::new);
        userModuleApi.enableTotp(userId, hashedCodes);

        // Only consume the temp token once enrollment fully succeeds, so a wrong code can be retried.
        consumeTempToken(jti, userId);
        userModuleApi.updateLastLogin(userId);

        return new TwoFactorEnrollmentResult(generateTokenResponse(user), recoveryCodes);
    }

    /**
     * Returns whether TOTP is enabled for the given user.
     */
    public boolean is2faEnabled(java.util.UUID userId) {
        return userModuleApi.isTotpEnabled(userId);
    }

    @Transactional
    public LoginResponse startImpersonation(java.util.UUID adminUserId, java.util.UUID targetUserId) {
        // 1. Verify admin exists and is SUPERADMIN
        var admin = userModuleApi.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (admin.role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Only SUPERADMIN can impersonate");
        }

        // 2. Check impersonation module is enabled
        if (!adminModuleApi.isModuleEnabled("impersonation")) {
            throw new ForbiddenException("Impersonation module is disabled");
        }

        // 3. Load target user
        var target = userModuleApi.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        // 4. Cannot impersonate another SUPERADMIN
        if (target.role() == UserRole.SUPERADMIN) {
            throw new BadRequestException("Cannot impersonate a SUPERADMIN");
        }

        // 5. Generate impersonation token (access token with impersonatedBy claim)
        String accessToken = jwtService.generateImpersonationToken(
                targetUserId, target.email(), target.role().name(), adminUserId);
        String refreshToken = refreshTokenService.createRefreshToken(adminUserId);

        // 6. Audit log
        log.info("IMPERSONATION_START: Admin {} impersonating user {} ({})",
                adminUserId, target.email(), targetUserId);

        return new LoginResponse(accessToken, refreshToken, target.id(), target.email(), target.role().name());
    }

    @Transactional
    public LoginResponse stopImpersonation(java.util.UUID adminUserId) {
        var admin = userModuleApi.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        log.info("IMPERSONATION_STOP: Admin {} returning to own session", adminUserId);

        return new LoginResponse(
                jwtService.generateAccessToken(adminUserId, admin.email(), admin.role().name()),
                refreshTokenService.createRefreshToken(adminUserId),
                admin.id(), admin.email(), admin.role().name());
    }

    /**
     * Atomically marks a 2FA temp token (identified by its "jti") as used so it cannot be
     * replayed within its 5-minute lifetime. Mirrors the replay-detection pattern used in
     * {@link TotpService} (TOTP codes) and {@link RefreshTokenService} (used refresh tokens).
     * A token without a jti (legacy/older issuance) is allowed through for backwards compatibility.
     */
    private void consumeTempToken(String jti, java.util.UUID userId) {
        if (jti == null) {
            return; // older temp tokens have no jti; cannot enforce single-use
        }
        Boolean wasAbsent = redisTemplate.opsForValue()
                .setIfAbsent(TWO_FA_TEMP_USED_PREFIX + jti, userId.toString(), Duration.ofMinutes(5));
        if (Boolean.FALSE.equals(wasAbsent)) {
            log.warn("2FA temp token replay detected for user {}", userId);
            throw new BusinessException("Token already used");
        }
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
