package com.monteweb.auth;

import java.util.UUID;

/**
 * Public API: Token response for cross-module use (e.g., role switching).
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String email,
        String role
) {
}
