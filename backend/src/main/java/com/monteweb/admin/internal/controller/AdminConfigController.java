package com.monteweb.admin.internal.controller;

import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.admin.internal.dto.UpdateConfigRequest;
import com.monteweb.admin.internal.model.AuditLogEntry;
import com.monteweb.admin.internal.service.AdminService;
import com.monteweb.admin.internal.service.AuditService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminConfigController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminConfigController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTenantConfig()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> updateConfig(@RequestBody UpdateConfigRequest request) {
        var config = adminService.updateConfig(request.schoolName(), request.logoUrl(), request.targetHoursPerFamily(), request.targetCleaningHours(),
                request.bundesland(), request.schoolVacations(), request.requireAssignmentConfirmation());
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PutMapping("/config/theme")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> updateTheme(@RequestBody Map<String, Object> theme) {
        var config = adminService.updateTheme(theme);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PutMapping("/config/modules")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> updateModules(@RequestBody Map<String, Boolean> modules) {
        var config = adminService.updateModules(modules);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PostMapping("/config/logo")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> uploadLogo(@RequestParam("file") MultipartFile file) {
        var config = adminService.uploadLogo(file);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogEntry>>> getAuditLog(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(auditService.findAll(pageable))));
    }
}
