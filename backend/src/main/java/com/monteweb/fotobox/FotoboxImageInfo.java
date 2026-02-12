package com.monteweb.fotobox;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: DTO for a fotobox image.
 */
public record FotoboxImageInfo(
        UUID id,
        UUID threadId,
        UUID uploadedBy,
        String uploadedByName,
        String originalFilename,
        String imageUrl,
        String thumbnailUrl,
        long fileSize,
        String contentType,
        Integer width,
        Integer height,
        String caption,
        int sortOrder,
        Instant createdAt
) {}
