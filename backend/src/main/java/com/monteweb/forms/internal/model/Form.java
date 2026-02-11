package com.monteweb.forms.internal.model;

import com.monteweb.forms.FormScope;
import com.monteweb.forms.FormStatus;
import com.monteweb.forms.FormType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "forms")
@Getter
@Setter
@NoArgsConstructor
public class Form {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormScope scope;

    @Column(name = "scope_id")
    private UUID scopeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormStatus status = FormStatus.DRAFT;

    @Column(nullable = false)
    private boolean anonymous = false;

    private LocalDate deadline;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

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
