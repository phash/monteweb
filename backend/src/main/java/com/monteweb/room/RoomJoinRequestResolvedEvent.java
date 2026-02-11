package com.monteweb.room;

import java.util.UUID;

/**
 * Public API: Event published when a room join request is approved or denied.
 */
public record RoomJoinRequestResolvedEvent(
        UUID requestId,
        UUID roomId,
        UUID requesterId,
        String roomName,
        boolean approved
) {
}
