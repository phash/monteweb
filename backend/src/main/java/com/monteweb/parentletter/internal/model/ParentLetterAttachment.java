package com.monteweb.parentletter.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parent_letter_attachments")
@Getter
@Setter
@NoArgsConstructor
public class ParentLetterAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "letter_id", nullable = false)
    private ParentLetter letter;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
