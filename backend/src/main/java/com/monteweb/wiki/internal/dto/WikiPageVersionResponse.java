package com.monteweb.wiki.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record WikiPageVersionResponse(
        UUID id,
        String title,
        String content,
        UUID editedBy,
        String editedByName,
        Instant createdAt
) {
}
