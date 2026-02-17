package com.monteweb.fundgrube.internal.service;

import com.monteweb.fundgrube.internal.repository.FundgrubeImageRepository;
import com.monteweb.fundgrube.internal.repository.FundgrubeItemRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Removes claimed Fundgrube items after their expiry time (claimed_at + 1 day).
 */
@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fundgrube.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FundgrubeCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FundgrubeCleanupService.class);

    private final FundgrubeItemRepository itemRepo;
    private final FundgrubeImageRepository imageRepo;
    private final FundgrubeStorageService storageService;

    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void deleteExpiredItems() {
        var now = Instant.now();
        var expired = itemRepo.findExpired(now);
        if (expired.isEmpty()) return;

        for (var item : expired) {
            imageRepo.findByItemIdOrderByCreatedAt(item.getId()).forEach(img -> {
                storageService.delete(img.getStoragePath());
                storageService.delete(img.getThumbnailPath());
            });
        }
        int deleted = itemRepo.deleteExpired(now);
        log.info("Fundgrube cleanup: deleted {} expired items", deleted);
    }
}
