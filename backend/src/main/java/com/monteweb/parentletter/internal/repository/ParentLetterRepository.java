package com.monteweb.parentletter.internal.repository;

import com.monteweb.parentletter.ParentLetterStatus;
import com.monteweb.parentletter.internal.model.ParentLetter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ParentLetterRepository extends JpaRepository<ParentLetter, UUID> {

    Page<ParentLetter> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    List<ParentLetter> findByRoomIdAndStatusOrderByCreatedAtDesc(UUID roomId, ParentLetterStatus status);

    List<ParentLetter> findByRoomIdOrderByCreatedAtDesc(UUID roomId);

    Page<ParentLetter> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    /**
     * Find letters that need a reminder to be sent:
     * - status = SENT
     * - deadline is set
     * - reminderSent = false
     * - deadline is on or before thresholdTime (reminder window has arrived or passed)
     */
    @Query("""
            SELECT l FROM ParentLetter l
            WHERE l.status = :status
              AND l.deadline IS NOT NULL
              AND l.reminderSent = false
              AND l.deadline <= :thresholdTime
            """)
    List<ParentLetter> findLettersNeedingReminder(
            @Param("status") ParentLetterStatus status,
            @Param("thresholdTime") Instant thresholdTime);

    /**
     * Find letters scheduled to be sent whose sendDate has arrived.
     */
    @Query("""
            SELECT l FROM ParentLetter l
            WHERE l.status = :status
              AND l.sendDate IS NOT NULL
              AND l.sendDate <= :now
            """)
    List<ParentLetter> findScheduledLettersReadyToSend(
            @Param("status") ParentLetterStatus status,
            @Param("now") Instant now);
}
