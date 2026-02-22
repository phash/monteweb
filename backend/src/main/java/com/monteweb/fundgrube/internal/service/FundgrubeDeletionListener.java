package com.monteweb.fundgrube.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up fundgrube data when a user account is deleted.
 * Deletes user's items and images from MinIO.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.fundgrube", name = "enabled", havingValue = "true")
public class FundgrubeDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(FundgrubeDeletionListener.class);

    private final FundgrubeService fundgrubeService;

    public FundgrubeDeletionListener(FundgrubeService fundgrubeService) {
        this.fundgrubeService = fundgrubeService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up fundgrube data for deleted user {}", event.userId());
        fundgrubeService.cleanupUserData(event.userId());
    }
}
