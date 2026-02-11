package com.monteweb.room.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_join_requests")
@Getter
@Setter
@NoArgsConstructor
public class RoomJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoomJoinRequestStatus status = RoomJoinRequestStatus.PENDING;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public RoomJoinRequest(UUID roomId, UUID userId, String message) {
        this.roomId = roomId;
        this.userId = userId;
        this.message = message;
    }
}
