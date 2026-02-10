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
}
