package com.monteweb.auth.internal.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_refresh_tokens:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final Duration refreshTokenExpiration;

    public RefreshTokenService(StringRedisTemplate redisTemplate,
                               @Value("${monteweb.jwt.refresh-token-expiration}") Duration refreshTokenExpiration) {
        this.redisTemplate = redisTemplate;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createRefreshToken(UUID userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Store token → userId mapping
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + token,
                userId.toString(),
                refreshTokenExpiration
        );

        // Track token in user's token set for revoke-all support
        String userKey = USER_TOKENS_PREFIX + userId;
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, refreshTokenExpiration);

        return token;
    }

    public UUID validateAndRotate(String refreshToken) {
        String key = TOKEN_PREFIX + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            return null;
        }

        // Delete old token (rotation)
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + userId, refreshToken);

        return UUID.fromString(userId);
    }

    /**
     * Revokes all refresh tokens for a user (e.g., on password change or security event).
     */
    public void revokeAllForUser(UUID userId) {
        String userKey = USER_TOKENS_PREFIX + userId;
        var tokens = redisTemplate.opsForSet().members(userKey);
        if (tokens != null && !tokens.isEmpty()) {
            var tokenKeys = tokens.stream()
                    .map(t -> TOKEN_PREFIX + t)
                    .toList();
            redisTemplate.delete(tokenKeys);
        }
        redisTemplate.delete(userKey);
    }

    public void revoke(String refreshToken) {
        String key = TOKEN_PREFIX + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);

        // Also remove from user's token set
        if (userId != null) {
            redisTemplate.opsForSet().remove(USER_TOKENS_PREFIX + userId, refreshToken);
        }
    }
}
