package com.monteweb.room.internal.dto;

import jakarta.validation.constraints.Size;

public record UpdateRoomRequest(
        @Size(max = 200) String name,
        String description
) {
}
