package com.monteweb.parentletter.internal.service;

import com.monteweb.parentletter.ParentLetterStatus;
import com.monteweb.parentletter.internal.repository.ParentLetterRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Handles two scheduled tasks for parent letters:
 * 1. Sending reminder notifications for letters approaching their deadline.
 * 2. Dispatching letters whose sendDate has arrived (SCHEDULED → SENT).
 *
 * Both tasks run daily at 07:00 (school-appropriate time).
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterReminderService {

    private static final Logger log = LoggerFactory.getLogger(ParentLetterReminderService.class);

    private final ParentLetterRepository letterRepository;
    private final ParentLetterService parentLetterService;

    /**
     * Daily at 07:00: dispatch all SCHEDULED letters whose sendDate has arrived.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void dispatchScheduledLetters() {
        Instant now = Instant.now();
        var readyLetters = letterRepository.findScheduledLettersReadyToSend(ParentLetterStatus.SCHEDULED, now);

        if (readyLetters.isEmpty()) {
            return;
        }

        log.info("Dispatching {} scheduled parent letter(s)", readyLetters.size());
        for (var letter : readyLetters) {
            try {
                // System dispatch — no specific user; use letter's creator as sender reference
                UUID dispatchedBy = letter.getCreatedBy();
                parentLetterService.dispatchScheduledLetter(letter, dispatchedBy);
                log.info("Dispatched parent letter '{}' (id={})", letter.getTitle(), letter.getId());
            } catch (Exception e) {
                log.error("Failed to dispatch parent letter {} (id={}): {}",
                        letter.getTitle(), letter.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Daily at 07:00 (same cron — Spring allows multiple methods with the same schedule):
     * Find SENT letters whose deadline is within reminderDays from now,
     * where a reminder hasn't been sent yet. Send reminder notifications.
     */
    @Scheduled(cron = "0 5 7 * * *")
    @Transactional
    public void sendReminders() {
        Instant now = Instant.now();
        // We query for all letters where deadline is in the past or within the near future;
        // The actual per-letter threshold is computed in the repository using letter.reminderDays.
        // Here we use a generous upper bound of 30 days to find candidates,
        // then delegate fine-grained filtering to the service.
        Instant upperBound = now.plus(30, ChronoUnit.DAYS);
        var candidateLetters = letterRepository.findLettersNeedingReminder(ParentLetterStatus.SENT, upperBound);

        if (candidateLetters.isEmpty()) {
            return;
        }

        log.info("Processing reminder candidates for {} parent letter(s)", candidateLetters.size());
        for (var letter : candidateLetters) {
            // Check if the per-letter reminder window has actually arrived:
            // deadline - reminderDays <= now
            if (letter.getDeadline() == null) {
                continue;
            }
            Instant reminderThreshold = letter.getDeadline()
                    .minus(letter.getReminderDays(), ChronoUnit.DAYS);
            if (!now.isBefore(reminderThreshold)) {
                try {
                    parentLetterService.sendRemindersForLetter(letter);
                    log.info("Sent reminders for parent letter '{}' (id={})",
                            letter.getTitle(), letter.getId());
                } catch (Exception e) {
                    log.error("Failed to send reminders for parent letter {} (id={}): {}",
                            letter.getTitle(), letter.getId(), e.getMessage(), e);
                }
            }
        }
    }

}
