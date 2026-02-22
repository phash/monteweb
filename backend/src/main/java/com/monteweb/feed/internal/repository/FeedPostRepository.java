package com.monteweb.feed.internal.repository;

import com.monteweb.feed.SourceType;
import com.monteweb.feed.internal.model.FeedPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeedPostRepository extends JpaRepository<FeedPost, UUID> {

    /**
     * Personal feed: posts from the user's rooms, sections of those rooms, school-wide, and board posts.
     * Filters out expired and parent-only posts based on the user's role.
     * Targeted posts (target_user_ids not null) are only shown to the specified users.
     */
    @Query(value = """
        SELECT p.* FROM feed_posts p
        WHERE (
            (p.source_type = 'ROOM' AND p.source_id IN (:roomIds))
            OR (p.source_type = 'SECTION' AND p.source_id IN (:sectionIds))
            OR p.source_type = 'SCHOOL'
            OR p.source_type = 'SYSTEM'
            OR p.source_type = 'BOARD'
        )
        AND (p.expires_at IS NULL OR p.expires_at > NOW())
        AND (p.is_parent_only = false OR :isParent = true)
        AND (p.target_user_ids IS NULL OR CAST(:userId AS UUID) = ANY(p.target_user_ids))
        ORDER BY p.is_pinned DESC, p.published_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM feed_posts p
        WHERE (
            (p.source_type = 'ROOM' AND p.source_id IN (:roomIds))
            OR (p.source_type = 'SECTION' AND p.source_id IN (:sectionIds))
            OR p.source_type = 'SCHOOL'
            OR p.source_type = 'SYSTEM'
            OR p.source_type = 'BOARD'
        )
        AND (p.expires_at IS NULL OR p.expires_at > NOW())
        AND (p.is_parent_only = false OR :isParent = true)
        AND (p.target_user_ids IS NULL OR CAST(:userId AS UUID) = ANY(p.target_user_ids))
        """,
        nativeQuery = true)
    Page<FeedPost> findPersonalFeed(
            @Param("roomIds") Collection<UUID> roomIds,
            @Param("sectionIds") Collection<UUID> sectionIds,
            @Param("isParent") boolean isParent,
            @Param("userId") UUID userId,
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

    List<FeedPost> findByAuthorId(UUID authorId);
}
