package com.monteweb.shared.exception;

public record UnhandledExceptionEvent(
    String type,
    String message,
    String stackTrace,
    String location
) {}
