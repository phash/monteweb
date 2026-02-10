package com.monteweb.room.internal.dto;

import com.monteweb.room.RoomRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId,
        @NotNull RoomRole role
) {
}
