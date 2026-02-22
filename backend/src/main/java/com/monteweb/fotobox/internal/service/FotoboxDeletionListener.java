package com.monteweb.fotobox.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up fotobox data when a user account is deleted.
 * Deletes user's images from MinIO, anonymizes threads.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.fotobox", name = "enabled", havingValue = "true")
public class FotoboxDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(FotoboxDeletionListener.class);

    private final FotoboxService fotoboxService;

    public FotoboxDeletionListener(FotoboxService fotoboxService) {
        this.fotoboxService = fotoboxService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up fotobox data for deleted user {}", event.userId());
        fotoboxService.cleanupUserData(event.userId());
    }
}
