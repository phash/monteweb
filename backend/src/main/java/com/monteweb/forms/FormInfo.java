package com.monteweb.forms;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FormInfo(
        UUID id,
        String title,
        String description,
        FormType type,
        FormScope scope,
        UUID scopeId,
        String scopeName,
        List<UUID> sectionIds,
        List<String> sectionNames,
        FormStatus status,
        boolean anonymous,
        LocalDate deadline,
        int questionCount,
        int responseCount,
        int targetCount,
        UUID createdBy,
        String creatorName,
        boolean hasUserResponded,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant closedAt
) {
}
