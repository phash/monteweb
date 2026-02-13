package com.monteweb.user.internal.dto;

import com.monteweb.user.UserRole;
import jakarta.validation.constraints.NotNull;

public record SwitchRoleRequest(
        @NotNull UserRole role
) {
}
