package com.monteweb.bookmark;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: Read-only bookmark information for cross-module use.
 */
public record BookmarkInfo(
        UUID id,
        UUID userId,
        String contentType,
        UUID contentId,
        Instant createdAt
) {
}
