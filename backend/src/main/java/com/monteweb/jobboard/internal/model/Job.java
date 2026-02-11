package com.monteweb.jobboard.internal.model;

import com.monteweb.jobboard.JobStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(length = 200)
    private String location;

    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "estimated_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "max_assignees", nullable = false)
    private int maxAssignees = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private JobStatus status = JobStatus.OPEN;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", length = 50)
    private String scheduledTime;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "contact_info", length = 300)
    private String contactInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "closed_at")
    private Instant closedAt;

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
