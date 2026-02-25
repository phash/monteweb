package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, UUID> {

    List<MessageAttachment> findByMessageId(UUID messageId);

    List<MessageAttachment> findByMessageIdIn(List<UUID> messageIds);

    List<MessageAttachment> findByCreatedAtBefore(Instant cutoff);
}
