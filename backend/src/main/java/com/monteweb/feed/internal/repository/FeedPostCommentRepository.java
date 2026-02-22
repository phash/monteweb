package com.monteweb.feed.internal.repository;

import com.monteweb.feed.internal.model.FeedPostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedPostCommentRepository extends JpaRepository<FeedPostComment, UUID> {

    Page<FeedPostComment> findByPostIdOrderByCreatedAtAsc(UUID postId, Pageable pageable);

    long countByPostId(UUID postId);

    List<FeedPostComment> findByAuthorId(UUID authorId);
}
