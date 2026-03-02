package com.monteweb.parentletter;

import java.time.Instant;
import java.util.UUID;

public record ParentLetterRecipientInfo(
    UUID id,
    UUID studentId,
    String studentName,
    UUID parentId,
    String parentName,
    String familyName,
    RecipientStatus status,
    Instant readAt,
    Instant confirmedAt,
    String confirmedByName,
    Instant reminderSentAt
) {}
