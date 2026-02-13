package com.monteweb.admin.internal.controller;

import com.monteweb.admin.internal.dto.SubmitErrorReportRequest;
import com.monteweb.admin.internal.service.ErrorReportService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/error-reports")
public class ErrorReportIngestionController {

    private final ErrorReportService errorReportService;

    public ErrorReportIngestionController(ErrorReportService errorReportService) {
        this.errorReportService = errorReportService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitErrorReport(
            @Valid @RequestBody SubmitErrorReportRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
        errorReportService.submitReport(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
