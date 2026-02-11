package com.monteweb.room.internal.controller;

import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.dto.*;
import com.monteweb.room.internal.model.RoomSettings;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;
    private final UserModuleApi userModuleApi;

    public RoomController(RoomService roomService, UserModuleApi userModuleApi) {
        this.roomService = roomService;
        this.userModuleApi = userModuleApi;
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<RoomInfo>>> getMyRooms() {
        UUID userId = SecurityUtils.requireCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(roomService.findMyRooms(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RoomInfo>>> getAllRooms(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @PageableDefault(size = 20) Pageable pageable) {
        if (includeArchived) {
            requireSuperAdmin();
            return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(roomService.findAllIncludingArchived(pageable))));
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(roomService.findAll(pageable))));
    }

    // ── Interest rooms: browse & search ─────────────────────────────────

    @GetMapping("/discover")
    public ResponseEntity<ApiResponse<PageResponse<RoomInfo>>> browseDiscoverable(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {
        var page = (q != null && !q.isBlank())
                ? roomService.searchDiscoverable(q, pageable)
                : roomService.browseDiscoverable(pageable);
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
        boolean isSuperAdmin = user.isPresent() && user.get().role() == UserRole.SUPERADMIN;
        boolean isMember = roomService.isUserInRoom(userId, id);

        if (isSuperAdmin || isMember) {
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
        var room = roomService.updateInterestFields(id, request.tags(), request.discoverable(), expiresAt);
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
        if (user.isPresent() && user.get().role() == UserRole.SUPERADMIN) {
            return;
        }
        var role = roomService.getUserRoleInRoom(userId, roomId);
        if (role.isEmpty() || role.get() != RoomRole.LEADER) {
            throw new ForbiddenException("Only room leaders can perform this action");
        }
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
                    return new RoomDetailResponse.MemberResponse(
                            uid,
                            userOpt.map(UserInfo::displayName).orElse("Unknown"),
                            userOpt.map(UserInfo::avatarUrl).orElse(null),
                            roleOpt.orElse(RoomRole.MEMBER),
                            null
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
                room.discoverable(),
                room.tags()
        );
    }

    // ── Request DTOs ────────────────────────────────────────────────────

    public record CreateInterestRoomRequest(
            @jakarta.validation.constraints.NotBlank String name,
            String description,
            List<String> tags,
            String expiresAt
    ) {}

    public record UpdateInterestFieldsRequest(
            List<String> tags,
            Boolean discoverable,
            String expiresAt
    ) {}
}
