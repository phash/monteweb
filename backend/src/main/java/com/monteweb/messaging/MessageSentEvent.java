package com.monteweb.messaging;

import java.util.List;
import java.util.UUID;

/**
 * Event published when a new message is sent.
 * Consumed by notification module to create notifications.
 */
public record MessageSentEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String contentPreview,
        List<UUID> recipientIds
) {
}
