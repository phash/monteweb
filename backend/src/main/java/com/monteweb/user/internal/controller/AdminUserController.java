package com.monteweb.user.internal.controller;

import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserRole;
import com.monteweb.user.internal.dto.AdminUpdateProfileRequest;
import com.monteweb.user.internal.dto.UpdateAssignedRolesRequest;
import com.monteweb.user.internal.dto.UpdateRoleRequest;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.internal.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('SUPERADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final RoomModuleApi roomModuleApi;
    private final FamilyModuleApi familyModuleApi;

    public AdminUserController(UserService userService,
                               RoomModuleApi roomModuleApi,
                               FamilyModuleApi familyModuleApi) {
        this.userService = userService;
        this.roomModuleApi = roomModuleApi;
        this.familyModuleApi = familyModuleApi;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserInfo>>> listUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = userService.findFiltered(role, active, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse<UserInfo>> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateProfileRequest request) {
        var user = userService.adminUpdateProfile(id, request.email(), request.firstName(), request.lastName(), request.phone());
        return ResponseEntity.ok(ApiResponse.ok(user));
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

    @GetMapping("/{id}/rooms")
    public ResponseEntity<ApiResponse<List<RoomInfo>>> getUserRooms(@PathVariable UUID id) {
        var rooms = roomModuleApi.findByUserId(id);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    @GetMapping("/{id}/families")
    public ResponseEntity<ApiResponse<List<FamilyInfo>>> getUserFamilies(@PathVariable UUID id) {
        var families = familyModuleApi.findByUserId(id);
        return ResponseEntity.ok(ApiResponse.ok(families));
    }

    @PostMapping("/{id}/families/{familyId}")
    public ResponseEntity<ApiResponse<Void>> addUserToFamily(
            @PathVariable UUID id,
            @PathVariable UUID familyId,
            @RequestBody Map<String, String> body) {
        String role = body.getOrDefault("role", "PARENT");
        familyModuleApi.adminAddMember(familyId, id, role);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/{id}/families/{familyId}")
    public ResponseEntity<ApiResponse<Void>> removeUserFromFamily(
            @PathVariable UUID id,
            @PathVariable UUID familyId) {
        familyModuleApi.adminRemoveMember(familyId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/assigned-roles")
    public ResponseEntity<ApiResponse<UserInfo>> updateAssignedRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAssignedRolesRequest request) {
        var user = userService.updateAssignedRoles(id, request.roles());
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @GetMapping("/search-special")
    public ResponseEntity<ApiResponse<List<UserInfo>>> findBySpecialRole(
            @RequestParam String role) {
        var users = userService.findBySpecialRoleContaining(role);
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PostMapping("/{id}/special-roles")
    public ResponseEntity<ApiResponse<UserInfo>> addSpecialRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String role = body.get("role");
        var user = userService.addSpecialRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    @DeleteMapping("/{id}/special-roles/{role}")
    public ResponseEntity<ApiResponse<UserInfo>> removeSpecialRole(
            @PathVariable UUID id,
            @PathVariable String role) {
        var user = userService.removeSpecialRole(id, role);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    // --- DSGVO / GDPR Admin endpoints ---

    @GetMapping("/{id}/data-export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportUserData(@PathVariable UUID id) {
        userService.logDataAccess(SecurityUtils.requireCurrentUserId(), id, "ADMIN_DATA_EXPORT", "Admin-initiated data export");
        var data = userService.exportUserData(id);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> requestDeletion(@PathVariable UUID id) {
        userService.logDataAccess(SecurityUtils.requireCurrentUserId(), id, "ADMIN_DELETION_REQUEST", "Admin-initiated deletion request");
        userService.requestDeletion(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deletion requested"));
    }

    @PostMapping("/{id}/cancel-deletion")
    public ResponseEntity<ApiResponse<Void>> cancelDeletion(@PathVariable UUID id) {
        userService.logDataAccess(SecurityUtils.requireCurrentUserId(), id, "ADMIN_DELETION_CANCEL", "Admin-cancelled deletion");
        userService.cancelDeletion(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deletion cancelled"));
    }

    @GetMapping("/{id}/deletion-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeletionStatus(@PathVariable UUID id) {
        var user = userService.findEntityById(id);
        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("deletionRequested", user.getDeletionRequestedAt() != null);
        status.put("deletionRequestedAt", user.getDeletionRequestedAt());
        status.put("scheduledDeletionAt", user.getScheduledDeletionAt());
        return ResponseEntity.ok(ApiResponse.ok(status));
    }
}
