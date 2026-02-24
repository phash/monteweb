package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.MessagePollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MessagePollVoteRepository extends JpaRepository<MessagePollVote, UUID> {

    List<MessagePollVote> findByOptionIdIn(Collection<UUID> optionIds);

    List<MessagePollVote> findByOptionIdInAndUserId(Collection<UUID> optionIds, UUID userId);

    void deleteByOptionIdInAndUserId(Collection<UUID> optionIds, UUID userId);
}
