package com.monteweb.forms.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up forms data when a user account is deleted.
 * Anonymizes form responses and deletes response tracking.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.forms", name = "enabled", havingValue = "true")
public class FormsDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(FormsDeletionListener.class);

    private final FormsService formsService;

    public FormsDeletionListener(FormsService formsService) {
        this.formsService = formsService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up forms data for deleted user {}", event.userId());
        formsService.cleanupUserData(event.userId());
    }
}
