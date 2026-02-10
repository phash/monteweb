package com.monteweb.auth.internal.service;

import com.monteweb.auth.internal.model.PasswordResetToken;
import com.monteweb.auth.internal.repository.PasswordResetTokenRepository;
import com.monteweb.shared.exception.BadRequestException;
import com.monteweb.user.UserModuleApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_EXPIRY_HOURS = 24;

    private final PasswordResetTokenRepository tokenRepository;
    private final UserModuleApi userModule;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserModuleApi userModule,
                                PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userModule = userModule;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Request a password reset. Generates a token and logs the reset URL.
     * In production, this would send an email instead.
     */
    @Transactional
    public void requestReset(String email) {
        var userOpt = userModule.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            // Don't reveal whether the email exists
            log.info("Password reset requested for non-existent email: {}", email);
            return;
        }

        var user = userOpt.get();
        var resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUserId(user.id());
        resetToken.setExpiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS));
        tokenRepository.save(resetToken);

        // Log the reset URL (in production, send via email)
        log.info("Password reset token generated for user {}: {}", email, resetToken.getToken());
    }

    /**
     * Confirm a password reset using the token.
     */
    @Transactional
    public void confirmReset(String token, String newPassword) {
        var resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Reset token has expired");
        }

        // Hash the new password and update the user
        String passwordHash = passwordEncoder.encode(newPassword);
        userModule.updatePasswordHash(resetToken.getUserId(), passwordHash);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for user ID: {}", resetToken.getUserId());
    }
}
