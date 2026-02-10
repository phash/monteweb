package com.monteweb.shared.dto;

import java.time.Instant;

public record ApiResponse<T>(
        T data,
        String message,
        boolean success,
        Instant timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, true, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(data, message, true, Instant.now());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(null, message, false, Instant.now());
    }
}
