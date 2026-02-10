package com.monteweb.jobboard.internal.controller;

import com.monteweb.jobboard.*;
import com.monteweb.jobboard.internal.service.JobboardService;
import com.monteweb.jobboard.internal.service.JobboardService.*;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobboardController {

    private final JobboardService jobboardService;

    public JobboardController(JobboardService jobboardService) {
        this.jobboardService = jobboardService;
    }

    // ---- Jobs ----

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobInfo>>> listJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<JobStatus> status,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = jobboardService.listJobs(category, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PageResponse<JobInfo>>> listMyJobs(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = jobboardService.listMyJobs(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getCategories()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JobInfo>> createJob(@RequestBody CreateJobRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var job = jobboardService.createJob(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobInfo>> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getJob(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobInfo>> updateJob(
            @PathVariable UUID id,
            @RequestBody UpdateJobRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.updateJob(id, userId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelJob(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        jobboardService.cancelJob(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ---- Assignments ----

    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<JobAssignmentInfo>>> getAssignments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getAssignmentsForJob(id)));
    }

    @PostMapping("/{id}/apply")
    public ResponseEntity<ApiResponse<JobAssignmentInfo>> applyForJob(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.applyForJob(id, userId)));
    }

    @PutMapping("/assignments/{assignmentId}/start")
    public ResponseEntity<ApiResponse<JobAssignmentInfo>> startAssignment(
            @PathVariable UUID assignmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.startAssignment(assignmentId, userId)));
    }

    @PutMapping("/assignments/{assignmentId}/complete")
    public ResponseEntity<ApiResponse<JobAssignmentInfo>> completeAssignment(
            @PathVariable UUID assignmentId,
            @RequestBody CompleteAssignmentRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(
                jobboardService.completeAssignment(assignmentId, userId, request.actualHours(), request.notes())));
    }

    @PutMapping("/assignments/{assignmentId}/confirm")
    public ResponseEntity<ApiResponse<JobAssignmentInfo>> confirmAssignment(
            @PathVariable UUID assignmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.confirmAssignment(assignmentId, userId)));
    }

    @DeleteMapping("/assignments/{assignmentId}")
    public ResponseEntity<ApiResponse<Void>> cancelAssignment(@PathVariable UUID assignmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        jobboardService.cancelAssignment(assignmentId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/my-assignments")
    public ResponseEntity<ApiResponse<List<JobAssignmentInfo>>> getMyAssignments() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getMyAssignments(userId)));
    }

    // ---- Hours / Reports ----

    @GetMapping("/family/{familyId}/hours")
    public ResponseEntity<ApiResponse<FamilyHoursInfo>> getFamilyHours(@PathVariable UUID familyId) {
        var hours = jobboardService.getFamilyHours(familyId)
                .orElseThrow(() -> new com.monteweb.shared.exception.ResourceNotFoundException("Family", familyId));
        return ResponseEntity.ok(ApiResponse.ok(hours));
    }

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<List<FamilyHoursInfo>>> getAllFamilyHoursReport() {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getAllFamilyHoursReport()));
    }

    @GetMapping("/report/summary")
    public ResponseEntity<ApiResponse<ReportSummary>> getReportSummary() {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getReportSummary()));
    }
}
