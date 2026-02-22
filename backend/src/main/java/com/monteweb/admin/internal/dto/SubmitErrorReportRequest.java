package com.monteweb.admin.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubmitErrorReportRequest(
    @NotBlank @Size(max = 10) String source,
    @Size(max = 500) String errorType,
    @NotBlank @Size(max = 2000) String message,
    @Size(max = 10000) String stackTrace,
    @Size(max = 1000) String location,
    @Size(max = 500) String userAgent,
    @Size(max = 2000) String requestUrl
) {}
