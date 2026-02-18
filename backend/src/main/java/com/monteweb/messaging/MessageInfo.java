package com.monteweb.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MessageInfo(
        UUID id,
        UUID conversationId,
        UUID senderId,
        String senderName,
        String content,
        Instant createdAt,
        List<MessageImageInfo> images,
        ReplyInfo replyTo
) {
    public record MessageImageInfo(
            UUID imageId,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
    }

    public record ReplyInfo(
            UUID messageId,
            UUID senderId,
            String senderName,
            String contentPreview,
            boolean hasImage
    ) {
    }
}
