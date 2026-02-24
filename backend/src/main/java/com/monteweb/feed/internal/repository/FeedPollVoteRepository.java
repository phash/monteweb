package com.monteweb.feed.internal.repository;

import com.monteweb.feed.internal.model.FeedPollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeedPollVoteRepository extends JpaRepository<FeedPollVote, UUID> {

    List<FeedPollVote> findByOptionIdIn(Collection<UUID> optionIds);

    List<FeedPollVote> findByOptionIdInAndUserId(Collection<UUID> optionIds, UUID userId);

    void deleteByOptionIdInAndUserId(Collection<UUID> optionIds, UUID userId);
}
