package com.monteweb.admin.internal.controller;

import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.admin.internal.dto.CsvImportResult;
import com.monteweb.admin.internal.dto.UpdateConfigRequest;
import com.monteweb.admin.internal.model.AuditLogEntry;
import com.monteweb.admin.internal.service.AdminService;
import com.monteweb.admin.internal.service.AnalyticsService;
import com.monteweb.admin.internal.service.AuditService;
import com.monteweb.admin.internal.service.CsvImportService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final CsvImportService csvImportService;
    private final AnalyticsService analyticsService;

    public AdminConfigController(AdminService adminService, AuditService auditService,
                                 CsvImportService csvImportService, AnalyticsService analyticsService) {
        this.adminService = adminService;
        this.auditService = auditService;
        this.csvImportService = csvImportService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTenantConfig()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> updateConfig(@RequestBody UpdateConfigRequest request) {
        var config = adminService.updateConfig(request.schoolName(), request.logoUrl(), request.targetHoursPerFamily(), request.targetCleaningHours(),
                request.bundesland(), request.schoolVacations(), request.requireAssignmentConfirmation(),
                request.multilanguageEnabled(), request.defaultLanguage(), request.availableLanguages(), request.requireUserApproval(),
                request.privacyPolicyText(), request.privacyPolicyVersion(),
                request.termsText(), request.termsVersion(),
                request.dataRetentionDaysNotifications(), request.dataRetentionDaysAudit(),
                request.schoolFullName(), request.schoolAddress(), request.schoolPrincipal(),
                request.techContactName(), request.techContactEmail(),
                request.twoFactorMode(), request.directoryAdminOnly(),
                request.ldapUrl(), request.ldapBaseDn(),
                request.ldapBindDn(), request.ldapBindPassword(),
                request.ldapUserSearchFilter(), request.ldapAttrEmail(),
                request.ldapAttrFirstName(), request.ldapAttrLastName(),
                request.ldapDefaultRole(), request.ldapUseSsl(),
                request.clamavHost(), request.clamavPort(),
                request.jitsiServerUrl(),
                request.wopiOfficeUrl());
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

    @PutMapping("/config/maintenance")
    public ResponseEntity<ApiResponse<TenantConfigInfo>> updateMaintenance(@RequestBody Map<String, Object> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("maintenanceEnabled"));
        String message = body.get("maintenanceMessage") instanceof String m ? m : null;
        var config = adminService.updateMaintenance(enabled, message);
        return ResponseEntity.ok(ApiResponse.ok(config));
    }

    @PostMapping("/ldap/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testLdapConnection(@RequestBody(required = false) Map<String, String> params) {
        try {
            var config = adminService.getTenantConfig();
            String url = params != null && params.containsKey("ldapUrl") ? params.get("ldapUrl") : config.ldapUrl();
            String baseDn = params != null && params.containsKey("ldapBaseDn") ? params.get("ldapBaseDn") : config.ldapBaseDn();
            String bindDn = params != null && params.containsKey("ldapBindDn") ? params.get("ldapBindDn") : config.ldapBindDn();
            String bindPassword = params != null && params.containsKey("ldapBindPassword") ? params.get("ldapBindPassword") : null;
            boolean useSsl = params != null && params.containsKey("ldapUseSsl") ? Boolean.parseBoolean(params.get("ldapUseSsl")) : config.ldapUseSsl();

            if (url == null || url.isBlank() || baseDn == null || baseDn.isBlank()) {
                return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "message", "LDAP URL and Base DN are required")));
            }

            // If no password provided in request, read from stored config
            if (bindPassword == null || bindPassword.isBlank()) {
                bindPassword = adminService.getLdapBindPassword();
            }

            adminService.testLdapConnection(url, baseDn, bindDn, bindPassword, useSsl);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", true, "message", "Connection successful")));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of("success", false, "message", e.getMessage() != null ? e.getMessage() : "Connection failed")));
        }
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getAnalytics()));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogEntry>>> getAuditLog(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(auditService.findAll(pageable))));
    }

    // --- CSV Import ---

    @PostMapping("/csv-import")
    public ResponseEntity<ApiResponse<CsvImportResult>> importCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        var result = csvImportService.processCsv(file, dryRun);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/csv-import/example")
    public ResponseEntity<byte[]> downloadExampleCsv() {
        byte[] csv = csvImportService.generateExampleCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-example.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
