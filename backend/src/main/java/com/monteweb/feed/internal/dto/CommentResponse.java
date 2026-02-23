package com.monteweb.feed.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        String authorName,
        String content,
        java.util.List<ReactionSummary> reactions,
        Instant createdAt
) {
    public record ReactionSummary(
            String emoji,
            long count,
            boolean userReacted
    ) {
    }
}
