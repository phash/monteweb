package com.monteweb.messaging;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the messaging module.
 * Other modules interact with messaging exclusively through this interface.
 */
public interface MessagingModuleApi {

    Optional<ConversationInfo> findConversationById(UUID conversationId, UUID userId);

    List<ConversationInfo> findConversationsByUser(UUID userId);

    long getTotalUnreadCount(UUID userId);

    /**
     * Creates a group conversation for room chat channels.
     * @param title The conversation title
     * @param creatorId The user who creates the conversation
     * @param memberIds All participants
     * @return The created conversation info
     */
    ConversationInfo createGroupConversation(String title, UUID creatorId, List<UUID> memberIds);

    /**
     * Checks if a conversation is muted by a specific user.
     */
    boolean isConversationMutedByUser(UUID conversationId, UUID userId);
}
