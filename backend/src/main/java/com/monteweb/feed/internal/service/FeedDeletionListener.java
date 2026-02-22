package com.monteweb.feed.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up feed data when a user account is deleted.
 * Deletes user's posts and comments.
 */
@Component
public class FeedDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(FeedDeletionListener.class);

    private final FeedService feedService;

    public FeedDeletionListener(FeedService feedService) {
        this.feedService = feedService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up feed data for deleted user {}", event.userId());
        feedService.cleanupUserData(event.userId());
    }
}
