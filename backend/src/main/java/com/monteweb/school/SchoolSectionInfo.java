package com.monteweb.school;

import java.util.UUID;

/**
 * Public API: Read-only school section information for cross-module use.
 */
public record SchoolSectionInfo(
        UUID id,
        String name,
        String slug,
        String description,
        int sortOrder,
        boolean active
) {
}
