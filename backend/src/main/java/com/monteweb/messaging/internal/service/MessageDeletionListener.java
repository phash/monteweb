package com.monteweb.messaging.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up messaging data when a user account is deleted.
 * Anonymizes messages (nullifies sender and content) and deletes message images from MinIO.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class MessageDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(MessageDeletionListener.class);

    private final MessagingService messagingService;

    public MessageDeletionListener(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up messaging data for deleted user {}", event.userId());
        messagingService.cleanupUserData(event.userId());
    }
}
