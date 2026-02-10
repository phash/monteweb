package com.monteweb.room.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "room_chat_channels",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "channel_type"}))
@Getter
@Setter
@NoArgsConstructor
public class RoomChatChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 30)
    private ChannelType channelType = ChannelType.MAIN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum ChannelType {
        MAIN,
        PARENTS,
        STUDENTS
    }
}
