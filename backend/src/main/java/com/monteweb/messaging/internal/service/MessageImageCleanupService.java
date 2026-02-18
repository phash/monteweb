package com.monteweb.messaging.internal.service;

import com.monteweb.messaging.internal.repository.MessageImageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class MessageImageCleanupService {

    private static final Logger log = LoggerFactory.getLogger(MessageImageCleanupService.class);
    private static final int RETENTION_DAYS = 90;

    private final MessageImageRepository imageRepository;
    private final MessageStorageService storageService;

    public MessageImageCleanupService(MessageImageRepository imageRepository,
                                       MessageStorageService storageService) {
        this.imageRepository = imageRepository;
        this.storageService = storageService;
    }

    @Scheduled(cron = "0 30 3 * * *") // Daily at 3:30 AM
    @Transactional
    public void deleteExpiredImages() {
        var cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        var expired = imageRepository.findByCreatedAtBefore(cutoff);

        if (expired.isEmpty()) {
            return;
        }

        log.info("Cleaning up {} message images older than {} days", expired.size(), RETENTION_DAYS);

        for (var image : expired) {
            storageService.delete(image.getStoragePath());
            storageService.delete(image.getThumbnailPath());
            imageRepository.delete(image);
        }

        log.info("Successfully cleaned up {} expired message images", expired.size());
    }
}
