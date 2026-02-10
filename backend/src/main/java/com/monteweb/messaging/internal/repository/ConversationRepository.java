package com.monteweb.messaging.internal.repository;

import com.monteweb.messaging.internal.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.id IN (SELECT cp.conversationId FROM ConversationParticipant cp WHERE cp.userId = :userId)
            ORDER BY c.updatedAt DESC
            """)
    List<Conversation> findByParticipantUserId(UUID userId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE c.isGroup = false
            AND c.id IN (SELECT cp.conversationId FROM ConversationParticipant cp WHERE cp.userId = :user1)
            AND c.id IN (SELECT cp.conversationId FROM ConversationParticipant cp WHERE cp.userId = :user2)
            """)
    List<Conversation> findDirectConversation(UUID user1, UUID user2);
}
