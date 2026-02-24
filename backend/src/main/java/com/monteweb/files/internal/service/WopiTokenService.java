package com.monteweb.files.internal.service;

import com.monteweb.files.internal.model.WopiToken;
import com.monteweb.files.internal.repository.WopiTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.files", name = "enabled", havingValue = "true")
public class WopiTokenService {

    private static final Logger log = LoggerFactory.getLogger(WopiTokenService.class);
    private static final int TOKEN_BYTES = 32; // 32 bytes = 64 hex chars
    private static final long TOKEN_EXPIRY_MINUTES = 30;

    private final WopiTokenRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public WopiTokenService(WopiTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Creates a new WOPI token for a file.
     */
    @Transactional
    public WopiToken createToken(UUID fileId, UUID userId, UUID roomId, String permissions) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);

        var wopiToken = new WopiToken();
        wopiToken.setToken(token);
        wopiToken.setFileId(fileId);
        wopiToken.setUserId(userId);
        wopiToken.setRoomId(roomId);
        wopiToken.setPermissions(permissions != null ? permissions : "EDIT");
        wopiToken.setExpiresAt(Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES));

        return tokenRepository.save(wopiToken);
    }

    /**
     * Validates a WOPI token. Returns the token if valid, null if expired or not found.
     */
    @Transactional(readOnly = true)
    public WopiToken validateToken(String token) {
        return tokenRepository.findById(token)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .orElse(null);
    }

    /**
     * Cleans up expired WOPI tokens every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpired() {
        int deleted = tokenRepository.deleteExpired(Instant.now());
        if (deleted > 0) {
            log.debug("Cleaned up {} expired WOPI tokens", deleted);
        }
    }
}
