package com.monteweb.admin.internal.controller;

import com.monteweb.admin.internal.dto.ErrorReportInfo;
import com.monteweb.admin.internal.dto.UpdateErrorStatusRequest;
import com.monteweb.admin.internal.dto.UpdateGithubConfigRequest;
import com.monteweb.admin.internal.service.ErrorReportService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/error-reports")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminErrorReportController {

    private final ErrorReportService errorReportService;

    public AdminErrorReportController(ErrorReportService errorReportService) {
        this.errorReportService = errorReportService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ErrorReportInfo>>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @PageableDefault(size = 20, sort = "lastSeenAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
            PageResponse.from(errorReportService.findAll(status, source, pageable))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ErrorReportInfo>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(errorReportService.findById(id)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ErrorReportInfo>> updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateErrorStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(errorReportService.updateStatus(id, request.status())));
    }

    @PostMapping("/{id}/github")
    public ResponseEntity<ApiResponse<ErrorReportInfo>> createGithubIssue(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(errorReportService.createGithubIssue(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        errorReportService.deleteReport(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/github-config")
    public ResponseEntity<ApiResponse<Void>> updateGithubConfig(
            @Valid @RequestBody UpdateGithubConfigRequest request) {
        errorReportService.updateGithubConfig(request.githubRepo(), request.githubPat());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
