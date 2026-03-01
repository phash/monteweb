package com.monteweb.notification.internal.repository;

import com.monteweb.notification.internal.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :id AND n.userId = :userId")
    int markAsRead(UUID id, UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(UUID userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :id AND n.userId = :userId")
    int deleteByIdAndUserId(UUID id, UUID userId);

    List<Notification> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID userId, Instant since);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@org.springframework.data.repository.query.Param("cutoff") Instant cutoff);
}
