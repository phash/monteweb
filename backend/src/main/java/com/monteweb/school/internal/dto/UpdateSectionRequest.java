package com.monteweb.school.internal.dto;

import jakarta.validation.constraints.Size;

public record UpdateSectionRequest(
        @Size(max = 100) String name,
        String description,
        Integer sortOrder
) {
}
