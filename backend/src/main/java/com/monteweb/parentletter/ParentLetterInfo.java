package com.monteweb.parentletter;

import java.time.Instant;
import java.util.UUID;

public record ParentLetterInfo(
    UUID id,
    String title,
    ParentLetterStatus status,
    UUID roomId,
    String roomName,
    UUID createdBy,
    String creatorName,
    Instant sendDate,
    Instant deadline,
    int totalRecipients,
    int confirmedCount,
    Instant createdAt,
    Instant updatedAt
) {}
