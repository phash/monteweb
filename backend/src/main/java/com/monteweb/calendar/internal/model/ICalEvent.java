package com.monteweb.calendar.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ical_events", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"subscription_id", "uid"})
})
@Getter
@Setter
@NoArgsConstructor
public class ICalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;

    @Column(nullable = false, length = 500)
    private String uid;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 300)
    private String location;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "start_time", length = 5)
    private String startTime;

    @Column(name = "end_time", length = 5)
    private String endTime;

    @Column(name = "all_day", nullable = false)
    private boolean allDay = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
