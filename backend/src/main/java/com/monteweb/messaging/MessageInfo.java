package com.monteweb.messaging;

import java.time.Instant;
import java.util.UUID;

public record MessageInfo(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        Instant createdAt
) {
}
