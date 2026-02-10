package com.monteweb.cleaning.internal.controller;

import com.monteweb.cleaning.CleaningConfigInfo;
import com.monteweb.cleaning.CleaningSlotInfo;
import com.monteweb.cleaning.internal.service.CleaningService;
import com.monteweb.shared.dto.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cleaning")
@ConditionalOnProperty(prefix = "monteweb.modules.cleaning", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPERADMIN', 'SECTION_ADMIN')")
public class CleaningAdminController {

    private final CleaningService cleaningService;

    // ── Config endpoints ────────────────────────────────────────────────

    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<CleaningConfigInfo>>> getConfigs(
            @RequestParam(required = false) UUID sectionId) {
        List<CleaningConfigInfo> configs = sectionId != null
                ? cleaningService.getConfigsBySection(sectionId)
                : cleaningService.getAllActiveConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs));
    }

    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<CleaningConfigInfo>> createConfig(
            @RequestBody CleaningService.CreateConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.createConfig(request)));
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<CleaningConfigInfo>> updateConfig(
            @PathVariable UUID id,
            @RequestBody CleaningService.UpdateConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.updateConfig(id, request)));
    }

    // ── Slot generation ─────────────────────────────────────────────────

    @PostMapping("/configs/{id}/generate")
    public ResponseEntity<ApiResponse<List<CleaningSlotInfo>>> generateSlots(
            @PathVariable UUID id,
            @RequestBody CleaningService.GenerateSlotsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                cleaningService.generateSlots(id, request.from(), request.to())));
    }

    // ── Slot management ─────────────────────────────────────────────────

    @PutMapping("/slots/{id}")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> updateSlot(
            @PathVariable UUID id,
            @RequestBody CleaningService.UpdateSlotRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.updateSlot(id, request)));
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelSlot(@PathVariable UUID id) {
        cleaningService.cancelSlot(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── QR Code ─────────────────────────────────────────────────────────

    @GetMapping("/slots/{id}/qr")
    public ResponseEntity<ApiResponse<QrTokenResponse>> getQrToken(@PathVariable UUID id) {
        String token = cleaningService.getQrToken(id);
        return ResponseEntity.ok(ApiResponse.ok(new QrTokenResponse(token)));
    }

    // ── Dashboard ───────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<CleaningService.DashboardInfo>> getDashboard(
            @RequestParam UUID sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getDashboard(sectionId, from, to)));
    }

    public record QrTokenResponse(String qrToken) {
    }
}
