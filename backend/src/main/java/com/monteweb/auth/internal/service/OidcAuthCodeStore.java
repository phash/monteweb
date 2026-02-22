package com.monteweb.auth.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Stores verified OIDC user info in Redis with a short-lived one-time-use code.
 * This prevents OIDC token forgery by ensuring the client can only exchange
 * server-verified OIDC claims for JWT tokens.
 */
@Service
@ConditionalOnProperty(prefix = "monteweb.oidc", name = "enabled", havingValue = "true")
public class OidcAuthCodeStore {

    private static final Logger log = LoggerFactory.getLogger(OidcAuthCodeStore.class);
    private static final String PREFIX = "oidc:code:";
    private static final Duration CODE_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redisTemplate;

    public OidcAuthCodeStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Stores verified OIDC user claims and returns a one-time-use code.
     */
    public String storeVerifiedClaims(String provider, String subject, String email,
                                       String firstName, String lastName) {
        String code = UUID.randomUUID().toString();
        String key = PREFIX + code;
        String value = String.join("\0", provider, subject, email,
                firstName != null ? firstName : "", lastName != null ? lastName : "");
        redisTemplate.opsForValue().set(key, value, CODE_TTL);
        log.debug("Stored OIDC auth code for email: {}", email);
        return code;
    }

    /**
     * Retrieves and deletes the verified OIDC claims for the given code (one-time use).
     * Returns null if the code is invalid or expired.
     */
    public VerifiedOidcClaims consumeCode(String code) {
        String key = PREFIX + code;
        String value = redisTemplate.opsForValue().getAndDelete(key);
        if (value == null) {
            log.warn("OIDC auth code not found or expired: {}", code);
            return null;
        }
        String[] parts = value.split("\0", 5);
        if (parts.length < 3) {
            log.error("Corrupt OIDC auth code data");
            return null;
        }
        return new VerifiedOidcClaims(
                parts[0], parts[1], parts[2],
                parts.length > 3 && !parts[3].isEmpty() ? parts[3] : null,
                parts.length > 4 && !parts[4].isEmpty() ? parts[4] : null
        );
    }

    public record VerifiedOidcClaims(String provider, String subject, String email,
                                      String firstName, String lastName) {}
}
