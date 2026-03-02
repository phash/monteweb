package com.monteweb.parentletter.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parent_letter_configs")
@Getter
@Setter
@NoArgsConstructor
public class ParentLetterConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * NULL means this is the global (school-wide) config.
     * Non-null means it is section-specific.
     */
    @Column(name = "section_id")
    private UUID sectionId;

    @Column(name = "letterhead_path", length = 500)
    private String letterheadPath;

    @Column(name = "signature_template", columnDefinition = "TEXT")
    private String signatureTemplate;

    @Column(name = "reminder_days", nullable = false)
    private int reminderDays = 3;

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
