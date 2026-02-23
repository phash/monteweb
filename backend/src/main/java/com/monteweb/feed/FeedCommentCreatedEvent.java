package com.monteweb.feed;

import java.util.UUID;

/**
 * Public API: Published when a new comment is added to a feed post.
 * The notification module listens to this to create mention notifications.
 */
public record FeedCommentCreatedEvent(
        UUID commentId,
        UUID postId,
        UUID authorId,
        String authorName,
        String content
) {
}
