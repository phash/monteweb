package com.monteweb.parentletter.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up parent letter data when a user account is deleted.
 * - Anonymizes letters created by the deleted user (nullifies createdBy).
 * - Removes all recipient entries where the deleted user was parent or student.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(ParentLetterDeletionListener.class);

    private final ParentLetterService parentLetterService;

    public ParentLetterDeletionListener(ParentLetterService parentLetterService) {
        this.parentLetterService = parentLetterService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up parent letter data for deleted user {}", event.userId());
        parentLetterService.cleanupUserData(event.userId());
    }
}
