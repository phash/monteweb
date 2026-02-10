package com.monteweb.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public API: Read-only conversation information for cross-module use.
 */
public record ConversationInfo(
        UUID id,
        String title,
        boolean isGroup,
        List<ParticipantInfo> participants,
        String lastMessage,
        Instant lastMessageAt,
        long unreadCount,
        Instant createdAt
) {
    public record ParticipantInfo(
            UUID userId,
            String displayName,
            Instant lastReadAt
    ) {
    }
}
