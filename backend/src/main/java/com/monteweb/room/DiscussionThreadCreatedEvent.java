package com.monteweb.room;

import java.util.UUID;

/**
 * Public API: Event published when a new discussion thread is created in a room.
 */
public record DiscussionThreadCreatedEvent(
        UUID threadId,
        UUID roomId,
        UUID createdBy,
        String creatorName,
        String threadTitle
) {
}
