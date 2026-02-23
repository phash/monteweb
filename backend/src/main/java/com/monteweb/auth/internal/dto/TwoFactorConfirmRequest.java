package com.monteweb.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorConfirmRequest(
        @NotBlank String code
) {
}
