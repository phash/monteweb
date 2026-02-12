package com.monteweb.fotobox.internal.service;

import com.monteweb.fotobox.FotoboxPermissionLevel;
import com.monteweb.fotobox.internal.model.FotoboxRoomSettings;
import com.monteweb.fotobox.internal.repository.FotoboxImageRepository;
import com.monteweb.fotobox.internal.repository.FotoboxRoomSettingsRepository;
import com.monteweb.fotobox.internal.repository.FotoboxThreadRepository;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "monteweb.modules", name = "fotobox.enabled", havingValue = "true")
@RequiredArgsConstructor
public class FotoboxPermissionService {

    private final RoomModuleApi roomModule;
    private final UserModuleApi userModule;
    private final FotoboxRoomSettingsRepository settingsRepo;
    private final FotoboxThreadRepository threadRepo;
    private final FotoboxImageRepository imageRepo;

    public FotoboxPermissionLevel getPermission(UUID userId, UUID roomId) {
        boolean isSuperAdmin = isSuperAdmin(userId);
        if (!isSuperAdmin && !roomModule.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("Not a member of this room");
        }
        var settings = settingsRepo.findByRoomId(roomId).orElse(null);
        if (settings == null || !settings.isEnabled()) {
            throw new ForbiddenException("Fotobox not enabled for this room");
        }
        if (isSuperAdmin) {
            return FotoboxPermissionLevel.CREATE_THREADS;
        }
        var role = roomModule.getUserRoleInRoom(userId, roomId).orElse(null);
        if (role == RoomRole.LEADER) {
            return FotoboxPermissionLevel.CREATE_THREADS;
        }
        return FotoboxPermissionLevel.valueOf(settings.getDefaultPermission());
    }

    public void requirePermission(UUID userId, UUID roomId, FotoboxPermissionLevel required) {
        var actual = getPermission(userId, roomId);
        if (actual.ordinal() < required.ordinal()) {
            throw new ForbiddenException("Insufficient fotobox permissions");
        }
    }

    public void requireRoomMember(UUID userId, UUID roomId) {
        if (!isSuperAdmin(userId) && !roomModule.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("Not a member of this room");
        }
    }

    public boolean isLeaderOrAdmin(UUID userId, UUID roomId) {
        if (isSuperAdmin(userId)) {
            return true;
        }
        var role = roomModule.getUserRoleInRoom(userId, roomId).orElse(null);
        return role == RoomRole.LEADER;
    }

    private boolean isSuperAdmin(UUID userId) {
        return userModule.findById(userId)
                .map(u -> u.role() == UserRole.SUPERADMIN)
                .orElse(false);
    }

    public boolean isThreadOwnerOrLeader(UUID userId, UUID threadId) {
        var thread = threadRepo.findById(threadId).orElse(null);
        if (thread == null) return false;
        if (userId.equals(thread.getCreatedBy())) return true;
        return isLeaderOrAdmin(userId, thread.getRoomId());
    }

    public boolean isImageOwnerOrLeader(UUID userId, UUID imageId) {
        var image = imageRepo.findById(imageId).orElse(null);
        if (image == null) return false;
        if (userId.equals(image.getUploadedBy())) return true;
        var thread = threadRepo.findById(image.getThreadId()).orElse(null);
        if (thread == null) return false;
        return isLeaderOrAdmin(userId, thread.getRoomId());
    }

    public FotoboxRoomSettings getSettingsOrDefault(UUID roomId) {
        return settingsRepo.findByRoomId(roomId).orElseGet(() -> {
            var settings = new FotoboxRoomSettings();
            settings.setRoomId(roomId);
            return settings;
        });
    }
}
