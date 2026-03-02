package com.monteweb.parentletter;

import java.util.UUID;

public record ParentLetterConfigInfo(
    UUID id,
    UUID sectionId,
    String sectionName,
    String letterheadPath,
    String signatureTemplate,
    int reminderDays
) {}
