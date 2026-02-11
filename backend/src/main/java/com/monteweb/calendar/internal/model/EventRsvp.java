package com.monteweb.calendar.internal.model;

import com.monteweb.calendar.RsvpStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_event_rsvps", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class EventRsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RsvpStatus status;

    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;

    @PrePersist
    protected void onCreate() {
        respondedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        respondedAt = Instant.now();
    }
}
