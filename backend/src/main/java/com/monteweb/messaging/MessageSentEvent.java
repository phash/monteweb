package com.monteweb.messaging;

import java.util.List;
import java.util.UUID;

public record MessageSentEvent(
        UUID messageId,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String contentPreview,
        String fullContent,
        List<UUID> recipientIds,
        boolean hasImage,
        boolean hasAttachment
) {
}
