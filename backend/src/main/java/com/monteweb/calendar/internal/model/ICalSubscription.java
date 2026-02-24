package com.monteweb.calendar.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ical_subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class ICalSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(length = 7)
    private String color = "#6366f1";

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
