package com.monteweb.forms;

import java.util.UUID;

public record FormClosedEvent(
        UUID formId,
        String title,
        FormScope scope,
        UUID scopeId
) {
}
