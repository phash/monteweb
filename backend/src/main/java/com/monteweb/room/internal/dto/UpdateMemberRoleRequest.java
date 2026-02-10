package com.monteweb.room.internal.dto;

import com.monteweb.room.RoomRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull RoomRole role
) {
}
