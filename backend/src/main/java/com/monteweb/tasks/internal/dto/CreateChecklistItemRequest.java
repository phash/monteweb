package com.monteweb.tasks.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChecklistItemRequest(
        @NotBlank @Size(max = 300) String title
) {
}
