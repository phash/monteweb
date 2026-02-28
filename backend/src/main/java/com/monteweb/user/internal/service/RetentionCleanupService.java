package com.monteweb.user.internal.service;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.notification.NotificationModuleApi;
import com.monteweb.user.internal.repository.DataAccessLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * DSGVO: Automatically cleans up old data based on retention policies.
 * Runs daily at 4:30 AM.
 */
@Component
public class RetentionCleanupService {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupService.class);

    private final AdminModuleApi adminModuleApi;
    private final DataAccessLogRepository dataAccessLogRepository;
    private final NotificationModuleApi notificationModuleApi;

    public RetentionCleanupService(AdminModuleApi adminModuleApi,
                                    DataAccessLogRepository dataAccessLogRepository,
                                    NotificationModuleApi notificationModuleApi) {
        this.adminModuleApi = adminModuleApi;
        this.dataAccessLogRepository = dataAccessLogRepository;
        this.notificationModuleApi = notificationModuleApi;
    }

    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void cleanupExpiredData() {
        var config = adminModuleApi.getTenantConfig();
        int notificationDays = config.dataRetentionDaysNotifications() != null
                ? config.dataRetentionDaysNotifications() : 90;
        int auditDays = config.dataRetentionDaysAudit() != null
                ? config.dataRetentionDaysAudit() : 1095;

        // Clean old data access logs
        Instant auditCutoff = Instant.now().minus(auditDays, ChronoUnit.DAYS);
        dataAccessLogRepository.deleteByCreatedAtBefore(auditCutoff);
        log.info("Cleaned data access logs older than {} days", auditDays);

        // Error report retention: RESOLVED/IGNORED after 90d, NEW/REPORTED after 365d
        Instant now = Instant.now();
        Instant cutoff90 = now.minus(90, ChronoUnit.DAYS);
        Instant cutoff365 = now.minus(365, ChronoUnit.DAYS);
        int deletedReports = adminModuleApi.cleanupOldErrorReports(cutoff90, cutoff365);
        log.info("Error report retention: deleted {} old error reports", deletedReports);
    }
}
