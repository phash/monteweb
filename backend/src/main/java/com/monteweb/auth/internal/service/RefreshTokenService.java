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

    private static final String KEY_PREFIX = "refresh_token:";
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

        redisTemplate.opsForValue().set(
                KEY_PREFIX + token,
                userId.toString(),
                refreshTokenExpiration
        );

        return token;
    }

    public UUID validateAndRotate(String refreshToken) {
        String key = KEY_PREFIX + refreshToken;
        String userId = redisTemplate.opsForValue().get(key);

        if (userId == null) {
            return null;
        }

        // Delete old token (rotation)
        redisTemplate.delete(key);

        return UUID.fromString(userId);
    }

    public void revokeAllForUser(UUID userId) {
        // In a production system, we'd track all tokens per user.
        // For now, individual revocation on logout is sufficient.
    }

    public void revoke(String refreshToken) {
        redisTemplate.delete(KEY_PREFIX + refreshToken);
    }
}
