package com.monteweb.auth.internal.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration accessTokenExpiration;

    public JwtService(@Value("${monteweb.jwt.secret}") String secret,
                      @Value("${monteweb.jwt.access-token-expiration}") Duration accessTokenExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "email", email,
                        "role", role
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    /**
     * Generates a short-lived (5 min) temp token for 2FA verification.
     * Contains user ID, email, and role — but type="2fa_temp" prevents use as access token.
     */
    public String generateTempToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of(
                        "email", email,
                        "role", role,
                        "type", "2fa_temp"
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(5))))
                .signWith(key)
                .compact();
    }

    /**
     * Validates a 2FA temp token and returns claims if valid.
     */
    public java.util.Optional<Claims> validateTempToken(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!"2fa_temp".equals(claims.get("type", String.class))) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid temp token: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /**
     * Generates a short-lived (5 min) image token for authenticated image access.
     * Uses a distinct "type" claim to prevent use as a regular access token.
     */
    public String generateImageToken(UUID userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claims(Map.of("type", "image"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(5))))
                .signWith(key)
                .compact();
    }

    /**
     * Validates a short-lived image token and returns the user ID if valid.
     */
    public java.util.Optional<String> validateImageToken(String token) {
        try {
            var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!"image".equals(claims.get("type", String.class))) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid image token: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
