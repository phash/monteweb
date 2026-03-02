package com.monteweb.parentletter;

import java.util.List;
import java.util.UUID;

public record ParentLetterSentEvent(
    UUID letterId,
    String title,
    UUID roomId,
    UUID sentBy,
    String senderName,
    List<UUID> parentUserIds
) {}
