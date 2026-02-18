package com.monteweb.cleaning.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cleaning_registrations")
@Getter
@Setter
@NoArgsConstructor
public class CleaningRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "checked_in", nullable = false)
    private boolean checkedIn = false;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "checked_out", nullable = false)
    private boolean checkedOut = false;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(name = "actual_minutes")
    private Integer actualMinutes;

    @Column(name = "no_show", nullable = false)
    private boolean noShow = false;

    @Column(name = "swap_offered", nullable = false)
    private boolean swapOffered = false;


    @Column(nullable = false)
    private boolean confirmed = false;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
