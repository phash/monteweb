package com.monteweb.feed.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String content,
        Instant createdAt
) {
}
