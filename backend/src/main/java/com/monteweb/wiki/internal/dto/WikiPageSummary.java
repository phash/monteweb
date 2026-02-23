package com.monteweb.wiki.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record WikiPageSummary(
        UUID id,
        String title,
        String slug,
        UUID parentId,
        boolean hasChildren,
        Instant updatedAt
) {
}
