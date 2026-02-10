package com.monteweb.user.internal.controller;

import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.user.UserInfo;
import com.monteweb.user.internal.dto.UpdateRoleRequest;
import com.monteweb.user.internal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserInfo>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        var page = userService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserInfo>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        var user = userService.updateRole(id, request.role());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserInfo>> setActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        var user = userService.setActive(id, active);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }
}
