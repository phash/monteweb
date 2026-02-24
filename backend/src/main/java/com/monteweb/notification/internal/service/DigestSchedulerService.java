package com.monteweb.notification.internal.service;

import com.monteweb.user.UserModuleApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@ConditionalOnProperty(name = "monteweb.email.enabled", havingValue = "true")
public class DigestSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(DigestSchedulerService.class);

    private final NotificationService notificationService;
    private final DigestEmailService digestEmailService;
    private final UserModuleApi userModuleApi;

    public DigestSchedulerService(NotificationService notificationService,
                                   DigestEmailService digestEmailService,
                                   UserModuleApi userModuleApi) {
        this.notificationService = notificationService;
        this.digestEmailService = digestEmailService;
        this.userModuleApi = userModuleApi;
    }

    @Scheduled(cron = "0 0 7 * * *")
    public void sendDigests() {
        log.info("Starting email digest processing");
        int sent = 0;

        var users = userModuleApi.findUsersForDigest();
        for (var user : users) {
            try {
                var since = user.digestLastSentAt() != null
                        ? user.digestLastSentAt()
                        : Instant.now().minus(1, ChronoUnit.DAYS);
                var notifications = notificationService.findUnreadSince(user.id(), since);
                if (!notifications.isEmpty()) {
                    digestEmailService.sendDigest(user.email(), user.firstName(), notifications);
                    userModuleApi.updateDigestSentAt(user.id(), Instant.now());
                    sent++;
                }
            } catch (Exception e) {
                log.error("Failed to send digest to user {}: {}", user.id(), e.getMessage());
            }
        }

        log.info("Email digest processing completed: {} digests sent", sent);
    }
}
