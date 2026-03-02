package com.monteweb.parentletter.internal.model;

import com.monteweb.parentletter.RecipientStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "parent_letter_recipients",
    uniqueConstraints = @UniqueConstraint(
        name = "unique_plr_letter_parent",
        columnNames = {"letter_id", "parent_id", "student_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class ParentLetterRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "letter_id", nullable = false)
    private ParentLetter letter;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "parent_id", nullable = false)
    private UUID parentId;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecipientStatus status = RecipientStatus.OPEN;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
