package com.monteweb.messaging;

import java.util.List;
import java.util.Map;
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

    /**
     * Adds a user as participant to an existing conversation (e.g. when joining a room with chat).
     */
    void addParticipantToConversation(UUID conversationId, UUID userId);

    /**
     * Removes a user from a conversation (e.g. when leaving a room with chat).
     */
    void removeParticipantFromConversation(UUID conversationId, UUID userId);

    /**
     * DSGVO: Export all messaging-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
