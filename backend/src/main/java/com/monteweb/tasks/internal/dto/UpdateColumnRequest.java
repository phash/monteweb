package com.monteweb.tasks.internal.dto;

import jakarta.validation.constraints.Size;

public record UpdateColumnRequest(
        @Size(max = 100) String name,
        Integer position
) {
}
