package com.monteweb.room;

import java.util.UUID;

/**
 * Public API: Event published when a new room is created.
 */
public record RoomCreatedEvent(
        UUID roomId,
        String roomName,
        String roomType,
        UUID createdBy
) {
}
