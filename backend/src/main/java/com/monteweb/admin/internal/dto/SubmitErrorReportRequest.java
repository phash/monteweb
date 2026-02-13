package com.monteweb.admin.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitErrorReportRequest(
    @NotBlank @Size(max = 10) String source,
    @Size(max = 500) String errorType,
    @NotBlank String message,
    String stackTrace,
    @Size(max = 1000) String location,
    String userAgent,
    @Size(max = 2000) String requestUrl
) {}
