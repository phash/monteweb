package com.monteweb.auth;

import com.monteweb.user.UserInfo;

/**
 * Public API: Facade interface for the auth module.
 * Other modules interact with auth exclusively through this interface.
 */
public interface AuthModuleApi {

    /**
     * Generates a new access+refresh token pair for a user.
     * Used when the user's role changes (e.g., role switching) and new tokens are needed.
     */
    TokenResponse generateTokensForUser(UserInfo user);

    /**
     * Validates a JWT token and extracts claims.
     * Returns empty if the token is invalid or expired.
     */
    java.util.Optional<TokenClaims> validateAndExtractClaims(String token);

    /**
     * Generates a short-lived (5 min) image token for authenticated image access.
     * These tokens are scoped to image endpoints only and cannot be used for API calls.
     */
    String generateImageToken(java.util.UUID userId);

    /**
     * Validates a short-lived image token and returns the user ID if valid.
     * Returns empty if the token is invalid, expired, or not an image token.
     */
    java.util.Optional<String> validateImageToken(String token);
}
