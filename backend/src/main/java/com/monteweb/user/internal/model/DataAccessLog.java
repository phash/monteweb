package com.monteweb.user.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "data_access_log")
@Getter
@Setter
@NoArgsConstructor
public class DataAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "accessed_by", nullable = false)
    private UUID accessedBy;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column
    private String details;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
