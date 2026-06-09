package com.monteweb.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to begin self-service 2FA enrollment during login when 2FA is MANDATORY
 * and the grace period has passed. Authenticated by the short-lived 2FA temp token
 * (not a full session), so a user who has no TOTP secret yet can still enroll.
 */
public record TwoFactorSetupTempRequest(
        @NotBlank String tempToken
) {
}
