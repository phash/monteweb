package com.monteweb.forms;

import java.util.UUID;

public record FormPublishedEvent(
        UUID formId,
        String title,
        FormType type,
        FormScope scope,
        UUID scopeId,
        UUID publishedBy,
        String publisherName
) {
}
