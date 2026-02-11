package com.monteweb.user.internal.dto;

import jakarta.validation.constraints.Email;

public record AdminUpdateProfileRequest(
        @Email String email,
        String firstName,
        String lastName,
        String phone
) {
}
