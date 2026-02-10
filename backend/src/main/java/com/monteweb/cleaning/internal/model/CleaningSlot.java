package com.monteweb.cleaning.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "cleaning_slots")
@Getter
@Setter
@NoArgsConstructor
public class CleaningSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "config_id", nullable = false)
    private UUID configId;

    @Column(name = "section_id", nullable = false)
    private UUID sectionId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "min_participants", nullable = false)
    private int minParticipants;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(nullable = false)
    private String status = "OPEN";

    @Column(nullable = false)
    private boolean cancelled = false;

    @Column(name = "qr_token")
    private String qrToken;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
