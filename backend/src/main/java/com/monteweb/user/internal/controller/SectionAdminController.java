package com.monteweb.user.internal.controller;

import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.school.SchoolModuleApi;
import com.monteweb.school.SchoolSectionInfo;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import com.monteweb.user.internal.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST controller for SECTION_ADMIN operations.
 * Section admins manage users and rooms within their school sections.
 * A section admin's sections are determined by rooms where they are a LEADER.
 */
@RestController
@RequestMapping("/api/v1/section-admin")
public class SectionAdminController {

    private final UserService userService;
    private final UserModuleApi userModuleApi;
    private final RoomModuleApi roomModuleApi;
    private final SchoolModuleApi schoolModuleApi;

    public SectionAdminController(UserService userService,
                                  UserModuleApi userModuleApi,
                                  RoomModuleApi roomModuleApi,
                                  SchoolModuleApi schoolModuleApi) {
        this.userService = userService;
        this.userModuleApi = userModuleApi;
        this.roomModuleApi = roomModuleApi;
        this.schoolModuleApi = schoolModuleApi;
    }

    /**
     * Returns the sections where the current user is SECTION_ADMIN.
     * Determined by rooms where the user is a LEADER -> extract distinct section IDs.
     */
    @GetMapping("/my-sections")
    public ResponseEntity<ApiResponse<List<SchoolSectionInfo>>> getMySections() {
        UUID userId = requireSectionAdminOrSuperAdmin();
        var sectionIds = getAdminSectionIds(userId);
        var sections = sectionIds.stream()
                .map(schoolModuleApi::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(SchoolSectionInfo::active)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(sections));
    }

    /**
     * Lists all users who are members of rooms in the given section.
     */
    @GetMapping("/sections/{sectionId}/users")
    public ResponseEntity<ApiResponse<List<UserInfo>>> getSectionUsers(@PathVariable UUID sectionId) {
        UUID userId = requireSectionAdminOrSuperAdmin();
        requireAccessToSection(userId, sectionId);

        var rooms = roomModuleApi.findBySectionId(sectionId);
        Set<UUID> userIds = new LinkedHashSet<>();
        for (RoomInfo room : rooms) {
            userIds.addAll(roomModuleApi.getMemberUserIds(room.id()));
        }

        var users = userModuleApi.findByIds(new ArrayList<>(userIds));
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    /**
     * Assigns a special role to a user. Only PUTZORGA and ELTERNBEIRAT are allowed.
     * The target user must belong to one of the admin's sections.
     */
    @PostMapping("/users/{targetUserId}/special-roles")
    public ResponseEntity<ApiResponse<UserInfo>> assignSpecialRole(
            @PathVariable UUID targetUserId,
            @RequestBody Map<String, String> body) {
        UUID userId = requireSectionAdminOrSuperAdmin();
        String role = body.get("role");
        validateAllowedRole(role);
        requireUserInAdminSections(userId, targetUserId);

        var user = userService.addSpecialRole(targetUserId, role);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    /**
     * Removes a special role from a user.
     */
    @DeleteMapping("/users/{targetUserId}/special-roles/{role}")
    public ResponseEntity<ApiResponse<UserInfo>> removeSpecialRole(
            @PathVariable UUID targetUserId,
            @PathVariable String role) {
        UUID userId = requireSectionAdminOrSuperAdmin();
        validateAllowedRole(role);
        requireUserInAdminSections(userId, targetUserId);

        var user = userService.removeSpecialRole(targetUserId, role);
        return ResponseEntity.ok(ApiResponse.ok(user));
    }

    /**
     * Lists all rooms in the given section.
     */
    @GetMapping("/sections/{sectionId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomInfo>>> getSectionRooms(@PathVariable UUID sectionId) {
        UUID userId = requireSectionAdminOrSuperAdmin();
        requireAccessToSection(userId, sectionId);

        var rooms = roomModuleApi.findBySectionId(sectionId);
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    /**
     * Creates a room in a section that the admin manages.
     */
    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<RoomInfo>> createRoom(@RequestBody Map<String, String> body) {
        UUID userId = requireSectionAdminOrSuperAdmin();

        String name = body.get("name");
        String description = body.get("description");
        String type = body.getOrDefault("type", "KLASSE");
        String sectionIdStr = body.get("sectionId");

        if (name == null || name.isBlank()) {
            throw new BusinessException("Room name is required");
        }
        if (sectionIdStr == null || sectionIdStr.isBlank()) {
            throw new BusinessException("Section ID is required");
        }

        UUID sectionId = UUID.fromString(sectionIdStr);
        requireAccessToSection(userId, sectionId);

        var room = roomModuleApi.createRoom(name, description, type, sectionId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(room));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private UUID requireSectionAdminOrSuperAdmin() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId)
                .orElseThrow(() -> new ForbiddenException("User not found"));
        if (user.role() != UserRole.SECTION_ADMIN && user.role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Only section admins or superadmins can perform this action");
        }
        return userId;
    }

    /**
     * Returns the set of section IDs the user administers.
     * Derived from rooms where the user is LEADER that have a non-null sectionId.
     */
    private Set<UUID> getAdminSectionIds(UUID userId) {
        var currentUser = userModuleApi.findById(userId);
        if (currentUser.isPresent() && currentUser.get().role() == UserRole.SUPERADMIN) {
            // Superadmins can access all active sections
            return schoolModuleApi.findAllActive().stream()
                    .map(SchoolSectionInfo::id)
                    .collect(Collectors.toSet());
        }

        var rooms = roomModuleApi.findByUserId(userId);
        Set<UUID> sectionIds = new LinkedHashSet<>();
        for (RoomInfo room : rooms) {
            if (room.sectionId() != null) {
                var role = roomModuleApi.getUserRoleInRoom(userId, room.id());
                if (role.isPresent() && role.get() == RoomRole.LEADER) {
                    sectionIds.add(room.sectionId());
                }
            }
        }
        return sectionIds;
    }

    private void requireAccessToSection(UUID userId, UUID sectionId) {
        var sectionIds = getAdminSectionIds(userId);
        if (!sectionIds.contains(sectionId)) {
            throw new ForbiddenException("You do not manage this section");
        }
    }

    private void requireUserInAdminSections(UUID adminUserId, UUID targetUserId) {
        var sectionIds = getAdminSectionIds(adminUserId);
        var targetRooms = roomModuleApi.findByUserId(targetUserId);
        boolean found = targetRooms.stream()
                .anyMatch(r -> r.sectionId() != null && sectionIds.contains(r.sectionId()));
        if (!found) {
            throw new ForbiddenException("Target user does not belong to any of your sections");
        }
    }

    private void validateAllowedRole(String role) {
        if (role == null || (!role.equals("PUTZORGA") && !role.equals("ELTERNBEIRAT")
                && !role.startsWith("PUTZORGA:") && !role.startsWith("ELTERNBEIRAT:"))) {
            throw new BusinessException("Only PUTZORGA and ELTERNBEIRAT roles are allowed");
        }
    }
}
