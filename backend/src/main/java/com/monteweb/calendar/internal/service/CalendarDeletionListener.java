package com.monteweb.calendar.internal.service;

import com.monteweb.user.UserDeletionExecutedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * DSGVO: Cleans up calendar data when a user account is deleted.
 * Anonymizes events (nullifies created_by), deletes RSVPs.
 */
@Component
@ConditionalOnProperty(prefix = "monteweb.modules.calendar", name = "enabled", havingValue = "true")
public class CalendarDeletionListener {

    private static final Logger log = LoggerFactory.getLogger(CalendarDeletionListener.class);

    private final CalendarService calendarService;

    public CalendarDeletionListener(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @Async
    @EventListener
    @Transactional
    public void onUserDeletion(UserDeletionExecutedEvent event) {
        log.info("Cleaning up calendar data for deleted user {}", event.userId());
        calendarService.cleanupUserData(event.userId());
    }
}
