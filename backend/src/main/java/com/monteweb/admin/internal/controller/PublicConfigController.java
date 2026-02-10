package com.monteweb.admin.internal.controller;

import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.admin.internal.service.AdminService;
import com.monteweb.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config")
public class PublicConfigController {

    private final AdminService adminService;

    public PublicConfigController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TenantConfigInfo>> getPublicConfig() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getTenantConfig()));
    }
}
