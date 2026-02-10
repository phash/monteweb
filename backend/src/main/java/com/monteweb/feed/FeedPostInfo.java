package com.monteweb.feed;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public API: Read-only feed post information for cross-module use.
 */
public record FeedPostInfo(
        UUID id,
        UUID authorId,
        String authorName,
        String title,
        String content,
        SourceType sourceType,
        UUID sourceId,
        String sourceName,
        boolean pinned,
        boolean parentOnly,
        int commentCount,
        List<AttachmentInfo> attachments,
        Instant publishedAt,
        Instant createdAt
) {
    public record AttachmentInfo(
            UUID id,
            String fileName,
            String fileUrl,
            String fileType,
            long fileSize
    ) {
    }
}
