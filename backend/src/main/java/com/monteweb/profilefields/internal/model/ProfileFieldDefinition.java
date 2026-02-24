package com.monteweb.profilefields.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_field_definitions")
@Getter
@Setter
@NoArgsConstructor
public class ProfileFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_key", nullable = false, unique = true, length = 50)
    private String fieldKey;

    @Column(name = "label_de", nullable = false, length = 200)
    private String labelDe;

    @Column(name = "label_en", nullable = false, length = 200)
    private String labelEn;

    @Column(name = "field_type", nullable = false, length = 20)
    private String fieldType;

    @Column(columnDefinition = "JSONB")
    private String options;

    @Column(nullable = false)
    private boolean required;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
