package com.monteweb.calendar;

import java.util.UUID;

/**
 * Public API: Event published when a calendar event is cancelled.
 */
public record EventCancelledEvent(
        UUID eventId,
        String eventTitle,
        EventScope scope,
        UUID scopeId,
        UUID cancelledBy,
        String cancellerName
) {
}
