package com.monteweb.family.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinFamilyRequest(
        @NotBlank String inviteCode
) {
}
