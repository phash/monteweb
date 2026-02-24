package com.monteweb.room;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public API: Read-only room information for cross-module use.
 */
public record RoomInfo(
        UUID id,
        String name,
        String description,
        String publicDescription,
        String avatarUrl,
        String type,
        UUID sectionId,
        boolean archived,
        int memberCount,
        String joinPolicy,
        Instant expiresAt,
        List<String> tags,
        String jitsiRoomName
) {
}
