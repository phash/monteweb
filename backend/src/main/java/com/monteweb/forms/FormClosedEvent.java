package com.monteweb.forms;

import java.util.List;
import java.util.UUID;

public record FormClosedEvent(
        UUID formId,
        String title,
        FormScope scope,
        UUID scopeId,
        List<UUID> sectionIds
) {
}
