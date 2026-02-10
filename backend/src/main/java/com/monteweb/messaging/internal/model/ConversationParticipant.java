package com.monteweb.messaging.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_participants")
@IdClass(ConversationParticipant.ConversationParticipantId.class)
@Getter
@Setter
@NoArgsConstructor
public class ConversationParticipant {

    @Id
    @Column(name = "conversation_id")
    private UUID conversationId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = Instant.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConversationParticipantId implements Serializable {
        private UUID conversationId;
        private UUID userId;
    }
}
