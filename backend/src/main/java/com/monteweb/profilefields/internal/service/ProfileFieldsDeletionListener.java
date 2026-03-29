package com.monteweb.profilefields.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up custom profile field values when a user account is deleted.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.profilefields", name = "enabled", havingValue = "true")
public class ProfileFieldsDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(ProfileFieldsDeletionListener.class);

    private final ProfileFieldsService profileFieldsService;

    public ProfileFieldsDeletionListener(ProfileFieldsService profileFieldsService) {
        this.profileFieldsService = profileFieldsService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up profile fields for deleted user {}", event.userId());
        profileFieldsService.cleanupUserData(event.userId());
    }
}
