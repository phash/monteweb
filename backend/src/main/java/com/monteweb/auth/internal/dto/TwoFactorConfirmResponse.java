package com.monteweb.auth.internal.dto;

import java.util.List;

public record TwoFactorConfirmResponse(
        List<String> recoveryCodes
) {
}
