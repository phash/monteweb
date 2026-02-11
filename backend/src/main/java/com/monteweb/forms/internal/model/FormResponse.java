package com.monteweb.forms.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "form_responses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"form_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class FormResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "form_id", nullable = false)
    private UUID formId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = Instant.now();
    }
}
