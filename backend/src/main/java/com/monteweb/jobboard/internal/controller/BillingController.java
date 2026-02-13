package com.monteweb.jobboard.internal.controller;

import com.monteweb.jobboard.internal.dto.BillingPeriodInfo;
import com.monteweb.jobboard.internal.dto.BillingReportInfo;
import com.monteweb.jobboard.internal.dto.CreateBillingPeriodRequest;
import com.monteweb.jobboard.internal.service.BillingService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing")
@ConditionalOnProperty(prefix = "monteweb.modules.jobboard", name = "enabled", havingValue = "true")
@PreAuthorize("hasRole('SUPERADMIN')")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/periods")
    public ResponseEntity<ApiResponse<List<BillingPeriodInfo>>> listPeriods() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.listPeriods()));
    }

    @GetMapping("/periods/active")
    public ResponseEntity<ApiResponse<BillingPeriodInfo>> getActivePeriod() {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getActivePeriod()));
    }

    @PostMapping("/periods")
    public ResponseEntity<ApiResponse<BillingPeriodInfo>> createPeriod(
            @Valid @RequestBody CreateBillingPeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.createPeriod(request)));
    }

    @GetMapping("/periods/{id}/report")
    public ResponseEntity<ApiResponse<BillingReportInfo>> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getReport(id)));
    }

    @PostMapping("/periods/{id}/close")
    public ResponseEntity<ApiResponse<BillingPeriodInfo>> closePeriod(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(billingService.closePeriod(id, userId)));
    }

    @GetMapping(value = "/periods/{id}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        byte[] pdf = billingService.exportPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jahresabrechnung.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping(value = "/periods/{id}/export/csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID id) {
        byte[] csv = billingService.exportCsv(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"jahresabrechnung.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
