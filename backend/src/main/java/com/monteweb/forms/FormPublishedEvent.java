package com.monteweb.forms;

import java.util.List;
import java.util.UUID;

public record FormPublishedEvent(
        UUID formId,
        String title,
        FormType type,
        FormScope scope,
        UUID scopeId,
        List<UUID> sectionIds,
        UUID publishedBy,
        String publisherName
) {
}
