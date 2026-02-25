package com.monteweb.messaging;

import com.monteweb.feed.PollInfo;

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
        List<MessageAttachmentInfo> attachments,
        ReplyInfo replyTo,
        List<ReactionSummary> reactions,
        PollInfo poll
) {
    public record MessageImageInfo(
            UUID imageId,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
    }

    public record MessageAttachmentInfo(
            UUID id,
            String attachmentType,
            String originalFilename,
            String contentType,
            Long fileSize,
            UUID linkedFileId,
            String linkedFileName,
            UUID linkedRoomId
    ) {
    }

    public record ReplyInfo(
            UUID messageId,
            UUID senderId,
            String senderName,
            String contentPreview,
            boolean hasImage,
            boolean hasAttachment
    ) {
    }

    public record ReactionSummary(
            String emoji,
            long count,
            boolean userReacted
    ) {
    }
}
