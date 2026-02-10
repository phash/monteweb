package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    Optional<Message> findFirstByConversationIdOrderByCreatedAtDesc(UUID conversationId);

    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.conversationId = :conversationId
            AND m.createdAt > :since
            AND m.senderId <> :userId
            """)
    long countUnreadMessages(UUID conversationId, UUID userId, Instant since);
}
