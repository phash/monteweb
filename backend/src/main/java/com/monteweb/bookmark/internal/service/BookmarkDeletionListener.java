package com.monteweb.bookmark.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up bookmark data when a user account is deleted.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.bookmarks", name = "enabled", havingValue = "true")
public class BookmarkDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(BookmarkDeletionListener.class);

    private final BookmarkService bookmarkService;

    public BookmarkDeletionListener(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up bookmarks for deleted user {}", event.userId());
        bookmarkService.cleanupUserData(event.userId());
    }
}
