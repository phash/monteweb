package com.monteweb.files;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: Read-only file information for cross-module use.
 */
public record FileInfo(
        UUID id,
        UUID roomId,
        UUID folderId,
        String originalName,
        String contentType,
        long fileSize,
        UUID uploadedBy,
        String uploaderName,
        String audience,
        Instant createdAt
) {
}
