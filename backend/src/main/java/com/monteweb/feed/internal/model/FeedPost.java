package com.monteweb.feed.internal.model;

import com.monteweb.feed.SourceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "feed_posts")
@Getter
@Setter
@NoArgsConstructor
public class FeedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    @Column(name = "is_parent_only", nullable = false)
    private boolean parentOnly = false;

    @Column(name = "target_user_ids", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private UUID[] targetUserIds;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<FeedPostAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<FeedPostComment> comments = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (publishedAt == null) publishedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
