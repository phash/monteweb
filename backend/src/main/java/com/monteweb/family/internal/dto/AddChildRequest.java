package com.monteweb.family.internal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddChildRequest(
        @NotNull UUID childUserId
) {
}
