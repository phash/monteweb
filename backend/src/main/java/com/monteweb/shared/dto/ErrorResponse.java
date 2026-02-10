package com.monteweb.shared.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        String error,
        String message,
        int status,
        Map<String, String> details,
        Instant timestamp
) {
    public static ErrorResponse of(String error, String message, int status) {
        return new ErrorResponse(error, message, status, null, Instant.now());
    }

    public static ErrorResponse of(String error, String message, int status, Map<String, String> details) {
        return new ErrorResponse(error, message, status, details, Instant.now());
    }
}
