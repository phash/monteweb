package com.monteweb.room.internal.controller;

import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.dto.*;
import com.monteweb.room.internal.model.RoomSettings;
import com.monteweb.room.internal.model.RoomType;
import com.monteweb.room.internal.service.RoomService;
import com.monteweb.shared.dto.ApiResponse;
import com.monteweb.shared.dto.PageResponse;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.shared.util.AvatarUtils;
import com.monteweb.shared.util.SecurityUtils;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final UserModuleApi userModuleApi;
    private final FamilyModuleApi familyModuleApi;

    public RoomController(RoomService roomService, UserModuleApi userModuleApi,
                          @org.springframework.beans.factory.annotation.Autowired(required = false) FamilyModuleApi familyModuleApi) {
        this.roomService = roomService;
        this.userModuleApi = userModuleApi;
        this.familyModuleApi = familyModuleApi;
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<RoomInfo>>> getMyRooms() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(roomService.findMyRooms(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoomInfo>>> getAllRooms(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) String type,
            @PageableDefault(size = 20) Pageable pageable) {
        if (type != null) {
            var roomType = RoomType.valueOf(type);
            var rooms = roomService.findByType(roomType);
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.fromList(rooms)));
        }
        if (includeArchived) {
            requireSuperAdmin();
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(roomService.findAllIncludingArchived(pageable))));
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(roomService.findAll(pageable))));
    }

    // ── Browse all rooms (non-member) ───────────────────────────────────

    @GetMapping("/browse")
    public ResponseEntity<ApiResponse<PageResponse<RoomInfo>>> browseRooms(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var page = (q != null && !q.isBlank())
                ? roomService.searchAllRooms(userId, q, pageable)
                : roomService.browseAllRooms(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    // ── Join requests ────────────────────────────────────────────────────

    @PostMapping("/{id}/join-request")
    public ResponseEntity<ApiResponse<JoinRequestInfo>> createJoinRequest(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        String message = body != null ? body.get("message") : null;
        var request = roomService.createJoinRequest(id, userId, message);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(request));
    }

    @GetMapping("/{id}/join-requests")
    public ResponseEntity<ApiResponse<List<JoinRequestInfo>>> getJoinRequests(@PathVariable UUID id) {
        requireLeaderOrAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok(roomService.getPendingJoinRequests(id)));
    }

    @PostMapping("/{id}/join-requests/{rid}/approve")
    public ResponseEntity<ApiResponse<JoinRequestInfo>> approveJoinRequest(
            @PathVariable UUID id, @PathVariable UUID rid) {
        requireLeaderOrAdmin(id);
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(roomService.approveJoinRequest(rid, userId)));
    }

    @PostMapping("/{id}/join-requests/{rid}/deny")
    public ResponseEntity<ApiResponse<JoinRequestInfo>> denyJoinRequest(
            @PathVariable UUID id, @PathVariable UUID rid) {
        requireLeaderOrAdmin(id);
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(roomService.denyJoinRequest(rid, userId)));
    }

    @GetMapping("/my-join-requests")
    public ResponseEntity<ApiResponse<List<JoinRequestInfo>>> getMyJoinRequests() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(roomService.getMyJoinRequests(userId)));
    }

    // ── Open rooms: browse & search ─────────────────────────────────

    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<PageResponse<RoomInfo>>> browseOpenRooms(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = (q != null && !q.isBlank())
                ? roomService.searchOpenRooms(q, pageable)
                : roomService.browseOpenRooms(pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomInfo>> create(@Valid @RequestBody CreateRoomRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var room = roomService.create(
                request.name(), request.description(), request.type(), request.sectionId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(room));
    }

    // ── Interest room creation (any authenticated user) ─────────────────

    @PostMapping("/interest")
    public ResponseEntity<ApiResponse<RoomInfo>> createInterestRoom(
            @Valid @RequestBody CreateInterestRoomRequest request) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        Instant expiresAt = request.expiresAt() != null ? Instant.parse(request.expiresAt()) : null;
        var room = roomService.createInterestRoom(
                request.name(), request.description(), request.tags(), expiresAt, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(room));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> getRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId);
        boolean isAdmin = user.isPresent() && (user.get().role() == UserRole.SUPERADMIN || user.get().role() == UserRole.SECTION_ADMIN);
        boolean isMember = roomService.isUserInRoom(userId, id);

        if (isAdmin || isMember) {
            return ResponseEntity.ok(ApiResponse.ok(buildDetailResponse(id)));
        }
        return ResponseEntity.ok(ApiResponse.ok(buildPublicResponse(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomInfo>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoomRequest request) {
        requireLeaderOrAdmin(id);
        var room = roomService.update(id, request.name(), request.description(), request.publicDescription(),
                request.type(), request.sectionId());
        return ResponseEntity.ok(ApiResponse.ok(room));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Void>> uploadRoomAvatar(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        requireLeaderOrAdmin(id);
        String dataUrl = AvatarUtils.validateAndConvert(file);
        roomService.updateAvatarUrl(id, dataUrl);
        return ResponseEntity.ok(ApiResponse.ok(null, "Avatar uploaded"));
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Void>> removeRoomAvatar(@PathVariable UUID id) {
        requireLeaderOrAdmin(id);
        roomService.updateAvatarUrl(id, null);
        return ResponseEntity.ok(ApiResponse.ok(null, "Avatar removed"));
    }

    @PutMapping("/{id}/settings")
    public ResponseEntity<ApiResponse<RoomInfo>> updateSettings(
            @PathVariable UUID id,
            @Valid @RequestBody RoomSettings settings) {
        requireLeaderOrAdmin(id);
        var room = roomService.updateSettings(id, settings);
        return ResponseEntity.ok(ApiResponse.ok(room));
    }

    // ── Interest room field updates ─────────────────────────────────────

    @PutMapping("/{id}/interest")
    public ResponseEntity<ApiResponse<RoomInfo>> updateInterestFields(
            @PathVariable UUID id,
            @RequestBody UpdateInterestFieldsRequest request) {
        requireLeaderOrAdmin(id);
        Instant expiresAt = request.expiresAt() != null ? Instant.parse(request.expiresAt()) : null;
        com.monteweb.room.JoinPolicy joinPolicy = request.joinPolicy() != null
                ? com.monteweb.room.JoinPolicy.valueOf(request.joinPolicy())
                : null;
        var room = roomService.updateInterestFields(id, request.tags(), joinPolicy, expiresAt);
        return ResponseEntity.ok(ApiResponse.ok(room));
    }

    // ── Join / Leave (self-service for discoverable rooms) ──────────────

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<Void>> joinRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        roomService.joinRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Joined room"));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        roomService.leaveRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Left room"));
    }

    // ── Member management (leader only) ─────────────────────────────────

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddMemberRequest request) {
        requireLeaderOrAdmin(id);
        roomService.addMember(id, request.userId(), request.role());
        return ResponseEntity.ok(ApiResponse.ok(null, "Member added"));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId) {
        requireLeaderOrAdmin(id);
        roomService.removeMember(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed"));
    }

    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        requireLeaderOrAdmin(id);
        roomService.updateMemberRole(id, userId, request.role());
        return ResponseEntity.ok(ApiResponse.ok(null, "Role updated"));
    }

    @PostMapping("/{id}/families/{familyId}")
    public ResponseEntity<ApiResponse<Void>> addFamily(
            @PathVariable UUID id,
            @PathVariable UUID familyId) {
        requireLeaderOrAdmin(id);
        int added = roomService.addFamilyMembers(id, familyId);
        return ResponseEntity.ok(ApiResponse.ok(null, added + " family members added"));
    }

    // ── Feed subscription (mute/unmute) ────────────────────────────────

    @PostMapping("/{id}/mute")
    public ResponseEntity<ApiResponse<Void>> muteRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        roomService.muteRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Room muted"));
    }

    @GetMapping("/{id}/jitsi")
    public ResponseEntity<ApiResponse<RoomInfo>> getJitsiRoom(@PathVariable UUID id) {
        var room = roomService.getJitsiRoom(id);
        return ResponseEntity.ok(ApiResponse.ok(room));
    }

    @PostMapping("/{id}/jitsi")
    public ResponseEntity<ApiResponse<RoomInfo>> generateJitsiRoom(@PathVariable UUID id) {
        var room = roomService.generateJitsiRoom(id);
        return ResponseEntity.ok(ApiResponse.ok(room));
    }

    @PostMapping("/{id}/unmute")
    public ResponseEntity<ApiResponse<Void>> unmuteRoom(@PathVariable UUID id) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        roomService.unmuteRoom(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Room unmuted"));
    }

    // ── Admin: archive / delete ────────────────────────────────────────

    @PutMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<RoomInfo>> toggleArchive(@PathVariable UUID id) {
        requireSuperAdmin();
        var room = roomService.toggleArchive(id);
        return ResponseEntity.ok(ApiResponse.ok(room));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable UUID id) {
        requireSuperAdmin();
        roomService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Room deleted"));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void requireSuperAdmin() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId);
        if (user.isEmpty() || user.get().role() != UserRole.SUPERADMIN) {
            throw new ForbiddenException("Only administrators can perform this action");
        }
    }

    private void requireLeaderOrAdmin(UUID roomId) {
        UUID userId = SecurityUtils.requireCurrentUserId();
        var user = userModuleApi.findById(userId);
        if (user.isPresent()) {
            var userRole = user.get().role();
            if (userRole == UserRole.SUPERADMIN) {
                return;
            }
            if (userRole == UserRole.SECTION_ADMIN) {
                var room = roomService.findById(roomId)
                        .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
                if (room.sectionId() != null && isAdminForSection(user.get(), room.sectionId())) {
                    return;
                }
            }
        }
        var role = roomService.getUserRoleInRoom(userId, roomId);
        if (role.isEmpty() || role.get() != RoomRole.LEADER) {
            throw new ForbiddenException("Only room leaders or administrators can perform this action");
        }
    }

    private boolean isAdminForSection(UserInfo user, UUID sectionId) {
        if (user.specialRoles() == null) return false;
        return user.specialRoles().contains("SECTION_ADMIN:" + sectionId);
    }

    private RoomDetailResponse buildDetailResponse(UUID roomId) {
        var room = roomService.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        var settings = roomService.getSettings(roomId);

        var memberUserIds = roomService.getMemberUserIds(roomId);
        var memberResponses = memberUserIds.stream()
                .map(uid -> {
                    var userOpt = userModuleApi.findById(uid);
                    var roleOpt = roomService.getUserRoleInRoom(uid, roomId);

                    // Look up the first family for this user (if family module is available)
                    UUID familyId = null;
                    String familyName = null;
                    if (familyModuleApi != null) {
                        var families = familyModuleApi.findByUserId(uid);
                        if (families != null && !families.isEmpty()) {
                            FamilyInfo family = families.get(0);
                            familyId = family.id();
                            familyName = family.name();
                        }
                    }

                    return new RoomDetailResponse.MemberResponse(
                            uid,
                            userOpt.map(UserInfo::displayName).orElse("Unknown"),
                            userOpt.map(UserInfo::avatarUrl).orElse(null),
                            roleOpt.orElse(RoomRole.MEMBER),
                            userOpt.map(u -> {
                                if (u.role() == null) return null;
                                // Only expose roles relevant for room display (teacher/student)
                                return switch (u.role()) {
                                    case TEACHER, STUDENT -> u.role().name();
                                    default -> null;
                                };
                            }).orElse(null),
                            null,
                            familyId,
                            familyName
                    );
                })
                .toList();

        return new RoomDetailResponse(
                room.id(),
                room.name(),
                room.description(),
                room.publicDescription(),
                room.avatarUrl(),
                room.type(),
                room.sectionId(),
                settings,
                room.archived(),
                null,
                null,
                memberResponses
        );
    }

    private RoomPublicResponse buildPublicResponse(UUID roomId) {
        var room = roomService.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
        return new RoomPublicResponse(
                room.id(),
                room.name(),
                room.publicDescription(),
                room.avatarUrl(),
                room.type(),
                room.sectionId(),
                room.memberCount(),
                room.joinPolicy(),
                room.tags()
        );
    }

    @PostMapping("/{id}/migrate-members")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> migrateMembers(
            @PathVariable UUID id,
            @RequestBody MigrateMembersRequest request) {
        requireLeaderOrAdmin(id);
        int count = roomService.migrateMembers(id, request.memberIds(), request.targetRoomId(), request.leaveSchool());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("migrated", count)));
    }

    // ── Request DTOs ────────────────────────────────────────────────────

    public record MigrateMembersRequest(
            List<UUID> memberIds,
            UUID targetRoomId,
            boolean leaveSchool
    ) {}

    public record CreateInterestRoomRequest(
            @jakarta.validation.constraints.NotBlank String name,
            String description,
            List<String> tags,
            String expiresAt
    ) {}

    public record UpdateInterestFieldsRequest(
            List<String> tags,
            String joinPolicy,
            String expiresAt
    ) {}
}
