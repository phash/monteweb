package com.monteweb.files;

import java.util.UUID;

/**
 * Public API: Published when a file is uploaded to a room.
 * The search module listens to this for Solr indexing.
 */
public record FileUploadedEvent(
        UUID fileId,
        UUID roomId,
        String originalName,
        String contentType,
        long fileSize,
        String storagePath,
        UUID uploadedBy
) {
}
