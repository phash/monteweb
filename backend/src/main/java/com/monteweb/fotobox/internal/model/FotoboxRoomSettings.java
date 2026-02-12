package com.monteweb.fotobox.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fotobox_room_settings")
@Getter
@Setter
@NoArgsConstructor
public class FotoboxRoomSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false, unique = true)
    private UUID roomId;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "default_permission", nullable = false, length = 20)
    private String defaultPermission = "VIEW_ONLY";

    @Column(name = "max_images_per_thread")
    private Integer maxImagesPerThread;

    @Column(name = "max_file_size_mb", nullable = false)
    private int maxFileSizeMb = 10;

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
