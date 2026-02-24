package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.MessagePoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessagePollRepository extends JpaRepository<MessagePoll, UUID> {

    Optional<MessagePoll> findByMessageId(UUID messageId);

    List<MessagePoll> findByMessageIdIn(Collection<UUID> messageIds);
}
