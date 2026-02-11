package com.monteweb.forms.internal.controller;

import com.monteweb.forms.*;
import com.monteweb.forms.internal.service.FormsService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms")
@ConditionalOnProperty(prefix = "monteweb.modules", name = "forms.enabled", havingValue = "true")
public class FormsController {

    private final FormsService formsService;

    public FormsController(FormsService formsService) {
        this.formsService = formsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FormInfo>>> getAvailableForms(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = formsService.getAvailableForms(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<PageResponse<FormInfo>>> getMyForms(
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = formsService.getMyForms(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FormDetailInfo>> createForm(
            @Valid @RequestBody CreateFormRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var form = formsService.createForm(request, userId);
        return ResponseEntity.ok(ApiResponse.ok(form));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FormDetailInfo>> getForm(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var form = formsService.getForm(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(form));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FormDetailInfo>> updateForm(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFormRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var form = formsService.updateForm(id, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(form));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForm(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        formsService.deleteForm(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<FormInfo>> publishForm(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var form = formsService.publishForm(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(form));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<FormInfo>> closeForm(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var form = formsService.closeForm(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(form));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<ApiResponse<Void>> submitResponse(
            @PathVariable UUID id,
            @Valid @RequestBody SubmitResponseRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        formsService.submitResponse(id, request, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}/results")
    public ResponseEntity<ApiResponse<FormResultsSummary>> getResults(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var results = formsService.getResults(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/{id}/responses")
    public ResponseEntity<ApiResponse<List<IndividualResponse>>> getIndividualResponses(
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var responses = formsService.getIndividualResponses(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
