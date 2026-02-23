package com.monteweb.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorDisableRequest(
        @NotBlank String password
) {
}
