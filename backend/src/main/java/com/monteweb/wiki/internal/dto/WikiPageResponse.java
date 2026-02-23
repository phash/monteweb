package com.monteweb.wiki.internal.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WikiPageResponse(
        UUID id,
        UUID roomId,
        UUID parentId,
        String title,
        String slug,
        String content,
        UUID createdBy,
        String createdByName,
        UUID lastEditedBy,
        String lastEditedByName,
        List<WikiPageSummary> children,
        Instant createdAt,
        Instant updatedAt
) {
}
