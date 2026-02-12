package com.monteweb.cleaning.internal.controller;

import com.monteweb.cleaning.CleaningConfigInfo;
import com.monteweb.cleaning.CleaningSlotInfo;
import com.monteweb.cleaning.internal.service.CleaningService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.util.PdfService;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cleaning")
@ConditionalOnProperty(prefix = "monteweb.modules.cleaning", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class CleaningAdminController {

    private final CleaningService cleaningService;
    private final PdfService pdfService;
    private final UserModuleApi userModuleApi;

    // ── Config endpoints ────────────────────────────────────────────────

    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<CleaningConfigInfo>>> getConfigs(
            @RequestParam(required = false) UUID sectionId) {
        requireCleaningAdmin(sectionId);
        List<CleaningConfigInfo> configs = sectionId != null
                ? cleaningService.getConfigsBySection(sectionId)
                : cleaningService.getAllActiveConfigs();
        return ResponseEntity.ok(ApiResponse.ok(configs));
    }

    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<CleaningConfigInfo>> createConfig(
            @RequestBody CleaningService.CreateConfigRequest request) {
        requireCleaningAdmin(request.sectionId());
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.createConfig(request)));
    }

    @PutMapping("/configs/{id}")
    public ResponseEntity<ApiResponse<CleaningConfigInfo>> updateConfig(
            @PathVariable UUID id,
            @RequestBody CleaningService.UpdateConfigRequest request) {
        var config = cleaningService.getConfigById(id);
        requireCleaningAdmin(config.sectionId());
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.updateConfig(id, request)));
    }

    // ── Slot generation ─────────────────────────────────────────────────

    @PostMapping("/configs/{id}/generate")
    public ResponseEntity<ApiResponse<List<CleaningSlotInfo>>> generateSlots(
            @PathVariable UUID id,
            @RequestBody CleaningService.GenerateSlotsRequest request) {
        var config = cleaningService.getConfigById(id);
        requireCleaningAdmin(config.sectionId());
        return ResponseEntity.ok(ApiResponse.ok(
                cleaningService.generateSlots(id, request.from(), request.to())));
    }

    // ── Slot management ─────────────────────────────────────────────────

    @PutMapping("/slots/{id}")
    public ResponseEntity<ApiResponse<CleaningSlotInfo>> updateSlot(
            @PathVariable UUID id,
            @RequestBody CleaningService.UpdateSlotRequest request) {
        requireCleaningAdminFromSlot(id);
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.updateSlot(id, request)));
    }

    @DeleteMapping("/slots/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelSlot(@PathVariable UUID id) {
        requireCleaningAdminFromSlot(id);
        cleaningService.cancelSlot(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── QR Code ─────────────────────────────────────────────────────────

    @GetMapping("/slots/{id}/qr")
    public ResponseEntity<ApiResponse<QrTokenResponse>> getQrToken(@PathVariable UUID id) {
        requireCleaningAdminFromSlot(id);
        String token = cleaningService.getQrToken(id);
        return ResponseEntity.ok(ApiResponse.ok(new QrTokenResponse(token)));
    }

    // ── QR Code PDF Export ──────────────────────────────────────────────

    @GetMapping(value = "/configs/{id}/qr-codes", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportQrCodesPdf(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var config = cleaningService.getConfigById(id);
        requireCleaningAdmin(config.sectionId());
        var slots = cleaningService.getSlotsByConfigAndDateRange(id, from, to);

        var entries = slots.stream()
                .map(s -> new PdfService.QrCodeEntry(
                        s.date().toString(),
                        s.startTime() + " - " + s.endTime(),
                        s.qrToken() != null ? s.qrToken() : "N/A"))
                .toList();

        byte[] pdf = pdfService.generateCleaningQrCodes(config.title(), entries);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"qr-codes-" + config.title() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    // ── Dashboard ───────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<CleaningService.DashboardInfo>> getDashboard(
            @RequestParam UUID sectionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        requireCleaningAdmin(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(cleaningService.getDashboard(sectionId, from, to)));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void requireCleaningAdmin(UUID sectionId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() == UserRole.SUPERADMIN || user.role() == UserRole.SECTION_ADMIN) {
            return;
        }
        if (user.specialRoles() != null) {
            // PUTZORGA special role (section-scoped)
            if (sectionId != null && user.specialRoles().contains("PUTZORGA:" + sectionId)) {
                return;
            }
            // ELTERNBEIRAT special role (can manage cleaning schedules)
            if (user.specialRoles().contains("ELTERNBEIRAT")) {
                return;
            }
            if (sectionId != null && user.specialRoles().contains("ELTERNBEIRAT:" + sectionId)) {
                return;
            }
        }
        throw new ForbiddenException("Not authorized for cleaning administration");
    }

    private void requireCleaningAdminFromSlot(UUID slotId) {
        var slot = cleaningService.getSlotById(slotId);
        requireCleaningAdmin(slot.sectionId());
    }

    public record QrTokenResponse(String qrToken) {
    }
}
