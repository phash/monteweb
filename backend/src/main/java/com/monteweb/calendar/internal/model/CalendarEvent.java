package com.monteweb.calendar.internal.model;

import com.monteweb.calendar.EventRecurrence;
import com.monteweb.calendar.EventScope;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "calendar_events")
@Getter
@Setter
@NoArgsConstructor
public class CalendarEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String location;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventScope scope;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventRecurrence recurrence = EventRecurrence.NONE;

    @Column(name = "recurrence_end")
    private LocalDate recurrenceEnd;

    @Column(nullable = false)
    private boolean cancelled = false;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType = "GENERAL";

    @Column(name = "created_by")
    private UUID createdBy;

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
