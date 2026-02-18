package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.MessageImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageImageRepository extends JpaRepository<MessageImage, UUID> {

    List<MessageImage> findByMessageId(UUID messageId);

    List<MessageImage> findByMessageIdIn(List<UUID> messageIds);

    List<MessageImage> findByCreatedAtBefore(Instant cutoff);
}
