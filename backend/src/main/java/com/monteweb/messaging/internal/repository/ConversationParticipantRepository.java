package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, ConversationParticipant.ConversationParticipantId> {

    List<ConversationParticipant> findByConversationId(UUID conversationId);

    List<ConversationParticipant> findByUserId(UUID userId);

    boolean existsByConversationIdAndUserId(UUID conversationId, UUID userId);

    @Modifying
    @Query("""
            UPDATE ConversationParticipant cp SET cp.lastReadAt = :now
            WHERE cp.conversationId = :conversationId AND cp.userId = :userId
            """)
    int markAsRead(UUID conversationId, UUID userId, Instant now);
}
