package com.monteweb.school.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSectionRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        int sortOrder
) {
}
