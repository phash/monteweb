package com.monteweb.cleaning.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up cleaning data when a user account is deleted.
 * Deletes cleaning registrations for the user.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.cleaning", name = "enabled", havingValue = "true")
public class CleaningDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(CleaningDeletionListener.class);

    private final CleaningService cleaningService;

    public CleaningDeletionListener(CleaningService cleaningService) {
        this.cleaningService = cleaningService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up cleaning registrations for deleted user {}", event.userId());
        cleaningService.cleanupUserData(event.userId());
    }
}
