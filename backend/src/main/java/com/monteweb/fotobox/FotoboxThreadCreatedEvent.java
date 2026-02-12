package com.monteweb.fotobox;

import java.util.UUID;

/**
 * Public API: Event published when a new fotobox thread is created.
 */
public record FotoboxThreadCreatedEvent(
        UUID threadId,
        UUID roomId,
        UUID createdBy,
        String threadTitle
) {}
