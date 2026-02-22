package com.monteweb.auth;

/**
 * Public API: Extracted claims from a validated JWT token.
 */
public record TokenClaims(
        String userId,
        String role
) {}
