package com.monteweb.auth.internal.service;

import com.monteweb.auth.internal.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Cleans up expired password reset tokens daily at 3 AM.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenCleanupScheduler.class);

    private final PasswordResetTokenRepository tokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteExpiredTokens() {
        int deleted = tokenRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired password reset tokens", deleted);
        }
    }
}
