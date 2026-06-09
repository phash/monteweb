package com.monteweb.bookmark;

import java.time.Instant;
import java.util.UUID;

/**
 * Public API: Read-only bookmark information for cross-module use.
 *
 * <p>{@code title} is a best-effort, denormalized label resolved from the
 * source module (e.g. feed post title, calendar event title). It may be
 * {@code null} when the source module is disabled, the source content was
 * deleted, or the content type has no resolvable title yet.
 */
public record BookmarkInfo(
        UUID id,
        UUID userId,
        String contentType,
        UUID contentId,
        String title,
        Instant createdAt
) {
}
