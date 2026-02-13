package com.monteweb.fotobox;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: DTO for a fotobox thread.
 */
public record FotoboxThreadInfo(
        UUID id,
        UUID roomId,
        String title,
        String description,
        UUID coverImageId,
        String coverImageThumbnailUrl,
        int imageCount,
        String audience,
        UUID createdBy,
        String createdByName,
        Instant createdAt
) {}
