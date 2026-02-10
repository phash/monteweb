package com.monteweb.feed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the feed module.
 * Other modules can publish posts and query the feed through this interface.
 */
public interface FeedModuleApi {

    /**
     * Creates a system-generated post (e.g., automatic announcements).
     */
    FeedPostInfo createSystemPost(String title, String content, SourceType sourceType, UUID sourceId);

    Optional<FeedPostInfo> findPostById(UUID postId);

    /**
     * Returns the personalized feed for a user, respecting room memberships and roles.
     */
    Page<FeedPostInfo> getPersonalFeed(UUID userId, Pageable pageable);
}
