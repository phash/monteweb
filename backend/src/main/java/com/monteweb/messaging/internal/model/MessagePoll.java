package com.monteweb.messaging.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "message_polls")
@Getter
@Setter
@NoArgsConstructor
public class MessagePoll {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(nullable = false)
    private boolean multiple = false;

    @Column(name = "closes_at")
    private Instant closesAt;

    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<MessagePollOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
