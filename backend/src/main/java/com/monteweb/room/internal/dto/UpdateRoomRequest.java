package com.monteweb.room.internal.dto;

import com.monteweb.room.internal.model.RoomType;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRoomRequest(
        @Size(max = 200) String name,
        String description,
        String publicDescription,
        RoomType type,
        UUID sectionId
) {
}
