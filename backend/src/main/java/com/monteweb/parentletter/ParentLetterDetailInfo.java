package com.monteweb.parentletter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ParentLetterDetailInfo(
    UUID id,
    String title,
    String content,
    ParentLetterStatus status,
    UUID roomId,
    String roomName,
    UUID createdBy,
    String creatorName,
    Instant sendDate,
    Instant deadline,
    int reminderDays,
    boolean reminderSent,
    int totalRecipients,
    int confirmedCount,
    List<ParentLetterRecipientInfo> recipients,
    Instant createdAt,
    Instant updatedAt
) {}
