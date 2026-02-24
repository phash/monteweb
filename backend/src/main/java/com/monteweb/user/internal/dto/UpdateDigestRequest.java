package com.monteweb.user.internal.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateDigestRequest(
        @Pattern(regexp = "NONE|DAILY|WEEKLY|BIWEEKLY|MONTHLY") String frequency
) {
}
