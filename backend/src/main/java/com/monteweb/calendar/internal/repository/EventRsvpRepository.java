package com.monteweb.calendar.internal.repository;

import com.monteweb.calendar.RsvpStatus;
import com.monteweb.calendar.internal.model.EventRsvp;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRsvpRepository extends JpaRepository<EventRsvp, UUID> {

    Optional<EventRsvp> findByEventIdAndUserId(UUID eventId, UUID userId);

    long countByEventIdAndStatus(UUID eventId, RsvpStatus status);

    @Query("SELECT DISTINCT r.eventId FROM EventRsvp r WHERE r.userId IN :userIds AND r.status IN ('ATTENDING', 'MAYBE')")
    List<UUID> findEventIdsByUserIdsAndAccepted(@Param("userIds") List<UUID> userIds);

    @Query("SELECT r.userId FROM EventRsvp r WHERE r.eventId = :eventId AND r.status = :status")
    List<UUID> findUserIdsByEventIdAndStatus(@Param("eventId") UUID eventId, @Param("status") RsvpStatus status);
}
