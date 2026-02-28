package com.monteweb.user.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DSGVO: Executes scheduled user deletions daily at 4:00 AM.
 * Users who requested deletion and whose grace period (14 days) has passed
 * will be anonymized and their data cleaned up across all modules.
 */
@Component
public class UserDeletionScheduler {

    private static final Logger log = LoggerFactory.getLogger(UserDeletionScheduler.class);

    private final UserService userService;

    public UserDeletionScheduler(UserService userService) {
        this.userService = userService;
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void executeScheduledDeletions() {
        var users = userService.findUsersScheduledForDeletion();
        if (users.isEmpty()) {
            return;
        }
        log.info("Executing {} scheduled user deletions", users.size());
        for (var user : users) {
            try {
                userService.anonymizeAndDelete(user.getId(), "Scheduled deletion after grace period");
                log.info("Deleted user {}", user.getId());
            } catch (Exception e) {
                log.error("Failed to delete user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
    }
}
