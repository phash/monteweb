package com.monteweb.room;

import java.util.UUID;

/**
 * Public API: Event published when a user requests to join a room.
 */
public record RoomJoinRequestEvent(
        UUID requestId,
        UUID roomId,
        UUID requesterId,
        String requesterName,
        String roomName
) {
}
