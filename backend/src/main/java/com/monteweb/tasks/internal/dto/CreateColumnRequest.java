package com.monteweb.tasks.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateColumnRequest(
        @NotBlank @Size(max = 100) String name
) {
}
