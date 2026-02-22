package com.monteweb.jobboard.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up jobboard data when a user account is deleted.
 * Anonymizes jobs, deletes assignments, cleans job attachments from MinIO.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobboardDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(JobboardDeletionListener.class);

    private final JobboardService jobboardService;

    public JobboardDeletionListener(JobboardService jobboardService) {
        this.jobboardService = jobboardService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up jobboard data for deleted user {}", event.userId());
        jobboardService.cleanupUserData(event.userId());
    }
}
