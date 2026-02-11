package com.monteweb.calendar;

import java.util.UUID;

/**
 * Public API: Event published when a new calendar event is created.
 */
public record EventCreatedEvent(
        UUID eventId,
        String eventTitle,
        EventScope scope,
        UUID scopeId,
        UUID createdBy,
        String creatorName
) {
}
