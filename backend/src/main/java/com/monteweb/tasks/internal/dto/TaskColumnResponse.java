package com.monteweb.tasks.internal.dto;

import java.util.UUID;

public record TaskColumnResponse(
        UUID id,
        String name,
        int position
) {
}
