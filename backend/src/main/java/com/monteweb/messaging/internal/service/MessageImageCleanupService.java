package com.monteweb.messaging.internal.service;

import com.monteweb.messaging.internal.model.MessageAttachment;
import com.monteweb.messaging.internal.repository.MessageAttachmentRepository;
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
    private final MessageAttachmentRepository attachmentRepository;
    private final MessageStorageService storageService;

    public MessageImageCleanupService(MessageImageRepository imageRepository,
                                       MessageAttachmentRepository attachmentRepository,
                                       MessageStorageService storageService) {
        this.imageRepository = imageRepository;
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
    }

    @Scheduled(cron = "0 30 3 * * *") // Daily at 3:30 AM
    @Transactional
    public void deleteExpiredImages() {
        var cutoff = Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        var expired = imageRepository.findByCreatedAtBefore(cutoff);

        if (!expired.isEmpty()) {
            log.info("Cleaning up {} message images older than {} days", expired.size(), RETENTION_DAYS);
            for (var image : expired) {
                storageService.delete(image.getStoragePath());
                storageService.delete(image.getThumbnailPath());
                imageRepository.delete(image);
            }
            log.info("Successfully cleaned up {} expired message images", expired.size());
        }

        // Also clean up expired file attachments (FILE type only, not FILE_LINK)
        var expiredAttachments = attachmentRepository.findByCreatedAtBefore(cutoff);
        var fileAttachments = expiredAttachments.stream()
                .filter(a -> a.getAttachmentType() == MessageAttachment.AttachmentType.FILE)
                .toList();
        if (!fileAttachments.isEmpty()) {
            log.info("Cleaning up {} message attachments older than {} days", fileAttachments.size(), RETENTION_DAYS);
            for (var att : fileAttachments) {
                storageService.delete(att.getStoragePath());
                attachmentRepository.delete(att);
            }
            log.info("Successfully cleaned up {} expired message attachments", fileAttachments.size());
        }
    }
}
