package com.monteweb.auth.internal.dto;

public record TwoFactorSetupResponse(
        String secret,
        String qrUri
) {
}
