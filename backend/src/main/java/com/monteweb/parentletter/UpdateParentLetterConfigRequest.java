package com.monteweb.parentletter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateParentLetterConfigRequest(
    String signatureTemplate,
    @Min(1) @Max(30) Integer reminderDays
) {}
