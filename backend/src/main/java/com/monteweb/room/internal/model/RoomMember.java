package com.monteweb.room.internal.model;

import com.monteweb.room.RoomRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_members")
@Getter
@Setter
@NoArgsConstructor
public class RoomMember {

    @EmbeddedId
    private RoomMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomRole role = RoomRole.MEMBER;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    public RoomMember(Room room, UUID userId, RoomRole role) {
        this.id = new RoomMemberId(room.getId(), userId);
        this.room = room;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }
}
