package com.monteweb.room.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record JoinRequestInfo(
        UUID id,
        UUID roomId,
        String roomName,
        UUID userId,
        String userName,
        String message,
        String status,
        Instant createdAt,
        Instant resolvedAt
) {
}
