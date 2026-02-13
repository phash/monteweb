package com.monteweb.calendar;

import java.util.List;
import java.util.UUID;

/**
 * Public API: Event published when a calendar event is deleted.
 * Includes the list of attending user IDs so listeners can create targeted notifications.
 */
public record EventDeletedEvent(
        UUID eventId,
        String eventTitle,
        EventScope scope,
        UUID scopeId,
        UUID deletedBy,
        String deleterName,
        List<UUID> attendingUserIds
) {
}
