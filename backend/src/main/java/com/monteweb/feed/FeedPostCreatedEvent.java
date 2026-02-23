package com.monteweb.feed;

import java.util.UUID;

/**
 * Public API: Published when a new feed post is created.
 * The notification module listens to this to create notifications.
 */
public record FeedPostCreatedEvent(
        UUID postId,
        UUID authorId,
        String authorName,
        String title,
        String content,
        SourceType sourceType,
        UUID sourceId
) {
}
