package com.monteweb.admin.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "error_reports")
@Getter
@Setter
@NoArgsConstructor
public class ErrorReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(nullable = false, length = 10)
    private String source;

    @Column(name = "error_type", length = 500)
    private String errorType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(length = 1000)
    private String location;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "request_url", length = 2000)
    private String requestUrl;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount = 1;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false, length = 20)
    private String status = "NEW";

    @Column(name = "github_issue_url", length = 500)
    private String githubIssueUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (firstSeenAt == null) firstSeenAt = now;
        if (lastSeenAt == null) lastSeenAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
