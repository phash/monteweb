package com.monteweb.parentletter;

public record ParentLetterStatsInfo(
    int activeCount,
    int totalRecipients,
    int totalConfirmed,
    int totalRead,
    int overdueCount
) {}
