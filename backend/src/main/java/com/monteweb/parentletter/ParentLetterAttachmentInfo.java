package com.monteweb.parentletter;

import java.time.Instant;
import java.util.UUID;

public record ParentLetterAttachmentInfo(
    UUID id,
    String originalFilename,
    String storagePath,
    long fileSize,
    String contentType,
    int sortOrder,
    Instant createdAt
) {}
