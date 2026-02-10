package com.monteweb.auth.internal.dto;

import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UUID userId,
        String email,
        String role
) {
}
