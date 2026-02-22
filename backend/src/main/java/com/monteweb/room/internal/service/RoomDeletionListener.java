package com.monteweb.room.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up room data when a user account is deleted.
 * Removes room memberships and join requests.
 */
@Component
public class RoomDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(RoomDeletionListener.class);

    private final RoomService roomService;

    public RoomDeletionListener(RoomService roomService) {
        this.roomService = roomService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up room data for deleted user {}", event.userId());
        roomService.cleanupUserData(event.userId());
    }
}
