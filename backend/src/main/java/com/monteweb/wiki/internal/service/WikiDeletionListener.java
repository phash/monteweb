package com.monteweb.wiki.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Anonymizes wiki data when a user account is deleted.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.wiki", name = "enabled", havingValue = "true")
public class WikiDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(WikiDeletionListener.class);

    private final WikiService wikiService;

    public WikiDeletionListener(WikiService wikiService) {
        this.wikiService = wikiService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Anonymizing wiki data for deleted user {}", event.userId());
        wikiService.cleanupUserData(event.userId());
    }
}
