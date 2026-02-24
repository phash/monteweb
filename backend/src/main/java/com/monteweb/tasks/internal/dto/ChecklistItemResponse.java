package com.monteweb.tasks.internal.dto;

import java.util.UUID;

public record ChecklistItemResponse(
        UUID id,
        String title,
        boolean checked,
        int position
) {
}
