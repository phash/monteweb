package com.monteweb.jobboard;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: Read-only job attachment information.
 */
public record JobAttachmentInfo(
        UUID id,
        UUID jobId,
        String originalFilename,
        long fileSize,
        String contentType,
        UUID uploadedBy,
        Instant createdAt
) {
}
