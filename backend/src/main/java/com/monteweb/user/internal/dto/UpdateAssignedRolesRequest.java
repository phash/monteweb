package com.monteweb.user.internal.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateAssignedRolesRequest(
        @NotNull Set<String> roles
) {
}
