package com.monteweb.auth.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String email,
        String role,
        Boolean requires2fa,
        String tempToken,
        Boolean requires2faSetup
) {
    /** Standard login response (no 2FA). */
    public LoginResponse(String accessToken, String refreshToken, UUID userId, String email, String role) {
        this(accessToken, refreshToken, userId, email, role, null, null, null);
    }

    /** 2FA challenge response — no real tokens, just a temp token. */
    public static LoginResponse twoFactorChallenge(String tempToken) {
        return new LoginResponse(null, null, null, null, null, true, tempToken, null);
    }

    /** 2FA setup required (MANDATORY mode, grace period passed). */
    public static LoginResponse twoFactorSetupRequired(String tempToken) {
        return new LoginResponse(null, null, null, null, null, null, tempToken, true);
    }
}
