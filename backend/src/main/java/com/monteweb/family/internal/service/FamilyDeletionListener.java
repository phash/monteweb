package com.monteweb.family.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up family data when a user account is deleted.
 * Removes from families, handles orphan families, deletes invitations.
 */
@Component
public class FamilyDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(FamilyDeletionListener.class);

    private final FamilyService familyService;

    public FamilyDeletionListener(FamilyService familyService) {
        this.familyService = familyService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up family data for deleted user {}", event.userId());
        familyService.cleanupUserData(event.userId());
    }
}
