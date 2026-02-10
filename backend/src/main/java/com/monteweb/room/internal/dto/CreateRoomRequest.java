package com.monteweb.room.internal.dto;

import com.monteweb.room.internal.model.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRoomRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull RoomType type,
        UUID sectionId
) {
}
