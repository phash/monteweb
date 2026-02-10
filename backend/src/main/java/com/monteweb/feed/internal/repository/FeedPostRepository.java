package com.monteweb.feed.internal.repository;

import com.monteweb.feed.SourceType;
import com.monteweb.feed.internal.model.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {

    /**
     * Personal feed: posts from the user's rooms, sections of those rooms, school-wide, and board posts.
     * Filters out expired and parent-only posts based on the user's role.
     */
    @Query("""
        SELECT p FROM FeedPost p
        WHERE (
            (p.sourceType = 'ROOM' AND p.sourceId IN :roomIds)
            OR (p.sourceType = 'SECTION' AND p.sourceId IN :sectionIds)
            OR p.sourceType = 'SCHOOL'
            OR p.sourceType = 'SYSTEM'
            OR p.sourceType = 'BOARD'
        )
        AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
        AND (p.parentOnly = false OR :isParent = true)
        ORDER BY p.pinned DESC, p.publishedAt DESC
    """)
    Page<FeedPost> findPersonalFeed(
            Collection<UUID> roomIds,
            Collection<UUID> sectionIds,
            boolean isParent,
            Pageable pageable
    );

    Page<FeedPost> findBySourceTypeAndSourceIdOrderByPinnedDescPublishedAtDesc(
            SourceType sourceType, UUID sourceId, Pageable pageable);

    List<FeedPost> findByPinnedTrueAndSourceTypeAndSourceIdOrderByPublishedAtDesc(
            SourceType sourceType, UUID sourceId);

    @Query("""
        SELECT p FROM FeedPost p
        WHERE p.sourceType = 'SYSTEM'
        AND (p.expiresAt IS NULL OR p.expiresAt > CURRENT_TIMESTAMP)
        AND p.pinned = true
        ORDER BY p.publishedAt DESC
    """)
    List<FeedPost> findActiveSystemBanners();
}
