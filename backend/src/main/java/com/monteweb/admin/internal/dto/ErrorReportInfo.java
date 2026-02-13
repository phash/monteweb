package com.monteweb.admin.internal.dto;

import java.time.Instant;
import java.util.UUID;

public record ErrorReportInfo(
    UUID id,
    String fingerprint,
    String source,
    String errorType,
    String message,
    String stackTrace,
    String location,
    UUID userId,
    String userAgent,
    String requestUrl,
    int occurrenceCount,
    Instant firstSeenAt,
    Instant lastSeenAt,
    String status,
    String githubIssueUrl,
    Instant createdAt
) {}
