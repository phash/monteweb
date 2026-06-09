package com.monteweb.auth.internal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Result of completing self-service 2FA enrollment during a MANDATORY-mode login.
 * Wraps the freshly issued session ({@link LoginResponse}) together with the one-time
 * recovery codes so the user is logged in immediately after enrolling and can store the
 * recovery codes (shown only once).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TwoFactorEnrollmentResult(
        LoginResponse login,
        List<String> recoveryCodes
) {
}
