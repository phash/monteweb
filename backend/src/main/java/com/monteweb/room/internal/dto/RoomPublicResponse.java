package com.monteweb.room.internal.dto;

import java.util.List;
import java.util.UUID;

/**
 * Limited room information returned to non-members.
 */
public record RoomPublicResponse(
        UUID id,
        String name,
        String publicDescription,
        String avatarUrl,
        String type,
        UUID sectionId,
        int memberCount,
        boolean discoverable,
        List<String> tags
) {
}
