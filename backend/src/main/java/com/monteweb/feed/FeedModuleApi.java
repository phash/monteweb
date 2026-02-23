package com.monteweb.feed;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
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

    /**
     * Creates a system-generated post visible only to the specified users.
     */
    FeedPostInfo createTargetedSystemPost(String title, String content, SourceType sourceType, UUID sourceId, List<UUID> targetUserIds);

    Optional<FeedPostInfo> findPostById(UUID postId);

    /**
     * Returns the personalized feed for a user, respecting room memberships and roles.
     */
    Page<FeedPostInfo> getPersonalFeed(UUID userId, Pageable pageable);

    /**
     * Searches feed posts by title or content (case-insensitive LIKE).
     * Used by the global search module.
     */
    List<FeedPostInfo> searchPosts(String query, int limit, UUID userId);

    /**
     * DSGVO: Export all feed-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
