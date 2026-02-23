package com.monteweb.feed.internal.repository;

import com.monteweb.feed.internal.model.FeedReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeedReactionRepository extends JpaRepository<FeedReaction, UUID> {

    Optional<FeedReaction> findByPostIdAndUserIdAndEmoji(UUID postId, UUID userId, String emoji);

    Optional<FeedReaction> findByCommentIdAndUserIdAndEmoji(UUID commentId, UUID userId, String emoji);

    List<FeedReaction> findByPostId(UUID postId);

    List<FeedReaction> findByCommentId(UUID commentId);

    @Query("SELECT r.postId FROM FeedReaction r WHERE r.postId IN :postIds GROUP BY r.postId")
    List<UUID> findPostIdsWithReactions(List<UUID> postIds);

    List<FeedReaction> findByPostIdIn(List<UUID> postIds);

    List<FeedReaction> findByCommentIdIn(List<UUID> commentIds);

    void deleteByUserId(UUID userId);
}
