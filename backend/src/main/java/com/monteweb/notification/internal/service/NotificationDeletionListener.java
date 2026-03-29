package com.monteweb.notification.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up notifications and push subscriptions when a user account is deleted.
 */
@Component
public class NotificationDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeletionListener.class);

    private final NotificationService notificationService;

    public NotificationDeletionListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up notifications for deleted user {}", event.userId());
        notificationService.cleanupUserData(event.userId());
    }
}
