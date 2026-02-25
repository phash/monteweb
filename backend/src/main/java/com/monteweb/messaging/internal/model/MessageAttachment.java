package com.monteweb.messaging.internal.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "message_attachments")
@Getter
@Setter
@NoArgsConstructor
public class MessageAttachment {

    public enum AttachmentType {
        FILE,       // Uploaded file (PDF etc.) stored in MinIO
        FILE_LINK   // Link to a room file (collaborative document)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", nullable = false, length = 20)
    private AttachmentType attachmentType;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    // For FILE type
    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "storage_path", length = 1000)
    private String storagePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 200)
    private String contentType;

    // For FILE_LINK type
    @Column(name = "linked_file_id")
    private UUID linkedFileId;

    @Column(name = "linked_file_name", length = 500)
    private String linkedFileName;

    @Column(name = "linked_room_id")
    private UUID linkedRoomId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
