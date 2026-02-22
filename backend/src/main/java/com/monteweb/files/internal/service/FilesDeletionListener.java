package com.monteweb.files.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up files data when a user account is deleted.
 * Deletes files uploaded by user from MinIO and database.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.files", name = "enabled", havingValue = "true")
public class FilesDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(FilesDeletionListener.class);

    private final FileService fileService;

    public FilesDeletionListener(FileService fileService) {
        this.fileService = fileService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up files for deleted user {}", event.userId());
        fileService.cleanupUserData(event.userId());
    }
}
