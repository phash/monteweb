package com.monteweb.jobboard.internal.controller;

import com.monteweb.jobboard.*;
import com.monteweb.jobboard.internal.model.JobAttachment;
import com.monteweb.jobboard.internal.repository.JobAttachmentRepository;
import com.monteweb.jobboard.internal.service.JobStorageService;
import com.monteweb.jobboard.internal.service.JobboardService;
import com.monteweb.jobboard.internal.service.JobboardService.*;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.shared.util.FileDownloadUtils;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
public class JobboardController {

    private final JobboardService jobboardService;
    private final UserModuleApi userModuleApi;
    private final JobAttachmentRepository attachmentRepository;
    private final JobStorageService storageService;

    public JobboardController(JobboardService jobboardService, UserModuleApi userModuleApi,
                              JobAttachmentRepository attachmentRepository, JobStorageService storageService) {
        this.jobboardService = jobboardService;
        this.userModuleApi = userModuleApi;
        this.attachmentRepository = attachmentRepository;
        this.storageService = storageService;
    }

    // ---- Jobs ----

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobInfo>>> listJobs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<JobStatus> status,
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = jobboardService.listJobs(category, status, eventId, roomId, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/by-event/{eventId}")
    public ResponseEntity<ApiResponse<List<JobInfo>>> getJobsByEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getJobsForEvent(eventId)));
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
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() == UserRole.STUDENT) {
            throw new ForbiddenException("Students cannot create jobs");
        }
        var job = jobboardService.createJob(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    @PutMapping("/{id}/link-event")
    public ResponseEntity<ApiResponse<JobInfo>> linkJobToEvent(
            @PathVariable UUID id,
            @RequestBody LinkEventRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var job = jobboardService.linkJobToEvent(id, request.eventId(), userId);
        return ResponseEntity.ok(ApiResponse.ok(job));
    }

    public record LinkEventRequest(UUID eventId) {}

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
    public ResponseEntity<ApiResponse<Void>> deleteOrCancelJob(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean permanent) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        if (permanent) {
            jobboardService.deleteJob(id, userId);
        } else {
            jobboardService.cancelJob(id, userId);
        }
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

    @PutMapping("/assignments/{assignmentId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectAssignment(
            @PathVariable UUID assignmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() != UserRole.TEACHER
                && user.role() != UserRole.SECTION_ADMIN
                && user.role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Not authorized");
        }
        jobboardService.rejectAssignment(assignmentId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/assignments/pending-confirmation")
    public ResponseEntity<ApiResponse<List<JobAssignmentInfo>>> getPendingConfirmations() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() != UserRole.TEACHER
                && user.role() != UserRole.SECTION_ADMIN
                && user.role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Not authorized");
        }
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getPendingConfirmations()));
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

    @GetMapping("/family/{familyId}/assignments")
    public ResponseEntity<ApiResponse<List<JobAssignmentInfo>>> getFamilyAssignments(@PathVariable UUID familyId) {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getAssignmentsForFamily(familyId)));
    }

    @GetMapping("/report")
    public ResponseEntity<ApiResponse<List<FamilyHoursInfo>>> getAllFamilyHoursReport() {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getAllFamilyHoursReport()));
    }

    @GetMapping("/report/summary")
    public ResponseEntity<ApiResponse<ReportSummary>> getReportSummary() {
        return ResponseEntity.ok(ApiResponse.ok(jobboardService.getReportSummary()));
    }

    // ---- Attachments ----

    @PostMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<JobAttachmentInfo>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        jobboardService.getJob(id); // verify job exists

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("File too large. Maximum size is 10 MB.");
        }

        int count = attachmentRepository.countByJobId(id);
        if (count >= 5) {
            throw new BusinessException("Maximum 5 attachments per job.");
        }

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String ext = JobStorageService.extensionFromContentType(contentType);
        if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")) {
            ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.') + 1);
        }

        var attachment = new JobAttachment();
        attachment.setJobId(id);
        attachment.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        attachment.setFileSize(file.getSize());
        attachment.setContentType(contentType);
        attachment.setUploadedBy(userId);

        attachment = attachmentRepository.save(attachment);

        String storagePath = storageService.upload(id, attachment.getId(), ext, file, contentType);
        attachment.setStoragePath(storagePath);
        attachment = attachmentRepository.save(attachment);

        var info = new JobAttachmentInfo(
                attachment.getId(), attachment.getJobId(), attachment.getOriginalFilename(),
                attachment.getFileSize(), attachment.getContentType(), attachment.getUploadedBy(),
                attachment.getCreatedAt());
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        SecurityUtils.requireCurrentUserId();
        var attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getJobId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        var stream = storageService.download(attachment.getStoragePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, FileDownloadUtils.buildContentDisposition("attachment", attachment.getOriginalFilename()))
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(attachment.getFileSize())
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getJobId().equals(id))
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        var user = userModuleApi.findById(userId).orElseThrow(() -> new ForbiddenException("User not found"));
        if (!attachment.getUploadedBy().equals(userId)
                && user.role() != UserRole.SUPERADMIN
                && user.role() != UserRole.TEACHER
                && user.role() != UserRole.SECTION_ADMIN) {
            throw new ForbiddenException("Not authorized to delete this attachment");
        }

        storageService.delete(attachment.getStoragePath());
        attachmentRepository.delete(attachment);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
