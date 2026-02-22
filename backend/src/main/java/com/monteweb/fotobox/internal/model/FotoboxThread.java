package com.monteweb.fotobox.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fotobox_threads")
@Getter
@Setter
@NoArgsConstructor
public class FotoboxThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image_id")
    private UUID coverImageId;

    @Column(nullable = false, length = 20)
    private String audience = "ALL";

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
