package com.monteweb.parentletter.internal.controller;

import com.monteweb.parentletter.*;
import com.monteweb.parentletter.internal.service.ParentLetterPdfService;
import com.monteweb.parentletter.internal.service.ParentLetterService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parent-letters")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "parentletter.enabled", havingValue = "true")
public class ParentLetterController {

    private final ParentLetterService parentLetterService;
    private final ParentLetterPdfService pdfService;

    public ParentLetterController(ParentLetterService parentLetterService,
                                  ParentLetterPdfService pdfService) {
        this.parentLetterService = parentLetterService;
        this.pdfService = pdfService;
    }

    /**
     * Create a new parent letter (DRAFT). Only LEADER or SUPERADMIN of the room.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ParentLetterDetailInfo>> createLetter(
            @Valid @RequestBody CreateParentLetterRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var result = parentLetterService.createLetter(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get all letters created by the current user (paginated).
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<ParentLetterInfo>>> getMyLetters(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = parentLetterService.getMyLetters(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    /**
     * Parent inbox: all SENT letters where the current user is a recipient.
     */
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<PageResponse<ParentLetterDetailInfo>>> getInbox(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = parentLetterService.getLettersForParent(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    /**
     * Stats for the current user's sent letters (teacher/admin).
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ParentLetterStatsInfo>> getStats() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var stats = parentLetterService.getStats(userId);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    /**
     * Get letter details.
     * - If the caller is the creator, LEADER, or SUPERADMIN: returns full detail with recipient list.
     * - If the caller is a parent recipient: returns detail with resolved content and marks as READ.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ParentLetterDetailInfo>> getLetter(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        // Try admin/creator view first; fall back to parent view on forbidden
        try {
            var detail = parentLetterService.getLetterDetail(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(detail));
        } catch (com.monteweb.shared.exception.ForbiddenException e) {
            // Try parent view (marks as read)
            var detail = parentLetterService.getLetterForParent(id, userId);
            return ResponseEntity.ok(ApiResponse.ok(detail));
        }
    }

    /**
     * Update a DRAFT letter (only creator/LEADER/SUPERADMIN).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ParentLetterDetailInfo>> updateLetter(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParentLetterRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var result = parentLetterService.updateLetter(id, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Send (or schedule) a DRAFT letter.
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<ParentLetterDetailInfo>> sendLetter(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var result = parentLetterService.sendLetter(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Close a SENT or SCHEDULED letter (no further confirmations accepted).
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<ParentLetterDetailInfo>> closeLetter(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var result = parentLetterService.closeLetter(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Delete a DRAFT letter.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLetter(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        parentLetterService.deleteLetter(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Parent confirms receipt for a specific student.
     */
    @PostMapping("/{id}/confirm/{studentId}")
    public ResponseEntity<ApiResponse<ParentLetterRecipientInfo>> confirmLetter(
            @PathVariable UUID id,
            @PathVariable UUID studentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var result = parentLetterService.confirmLetter(id, userId, studentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Explicitly mark a letter as read (for parents). Optional — reading via GET /{id} already marks as read.
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        parentLetterService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Download letter as PDF. If studentId is provided, variables are resolved for that student.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadLetterPdf(@PathVariable UUID id,
            @RequestParam(required = false) UUID studentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var detail = parentLetterService.getLetterDetail(id, userId);
        String resolved = parentLetterService.getResolvedContent(id, studentId, userId);
        byte[] pdf = pdfService.generateLetterPdf(detail, resolved);
        String safeName = detail.title().replaceAll("[^a-zA-Z0-9\\-]", "_");
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"Elternbrief-" + safeName + ".pdf\"")
                .body(pdf);
    }

    /**
     * Download tracking/status list as PDF.
     */
    @GetMapping("/{id}/tracking-pdf")
    public ResponseEntity<byte[]> downloadTrackingPdf(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var detail = parentLetterService.getLetterDetail(id, userId);
        byte[] pdf = pdfService.generateTrackingPdf(detail);
        String safeName = detail.title().replaceAll("[^a-zA-Z0-9\\-]", "_");
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"Ruecklauf-" + safeName + ".pdf\"")
                .body(pdf);
    }

    // ---- Attachments ----

    @PostMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<List<ParentLetterAttachmentInfo>>> uploadAttachments(
            @PathVariable UUID id,
            @RequestParam("files") List<MultipartFile> files) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var result = parentLetterService.uploadAttachments(id, files, userId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<List<ParentLetterAttachmentInfo>>> getAttachments(
            @PathVariable UUID id) {
        var result = parentLetterService.getAttachments(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var info = parentLetterService.getAttachmentInfo(attachmentId);
        var stream = parentLetterService.downloadAttachment(attachmentId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, info.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + info.originalFilename() + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(info.fileSize()))
                .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        parentLetterService.deleteAttachment(attachmentId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
