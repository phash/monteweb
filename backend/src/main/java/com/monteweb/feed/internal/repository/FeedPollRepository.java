package com.monteweb.feed.internal.repository;

import com.monteweb.feed.internal.model.FeedPoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedPollRepository extends JpaRepository<FeedPoll, UUID> {

    Optional<FeedPoll> findByPostId(UUID postId);

    List<FeedPoll> findByPostIdIn(Collection<UUID> postIds);
}
