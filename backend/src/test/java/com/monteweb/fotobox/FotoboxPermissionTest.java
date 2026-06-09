package com.monteweb.fotobox;

import com.monteweb.fotobox.internal.model.FotoboxRoomSettings;
import com.monteweb.fotobox.internal.repository.FotoboxImageRepository;
import com.monteweb.fotobox.internal.repository.FotoboxRoomSettingsRepository;
import com.monteweb.fotobox.internal.repository.FotoboxThreadRepository;
import com.monteweb.fotobox.internal.service.FotoboxPermissionService;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FotoboxPermissionService covering the permission hierarchy
 * (VIEW_ONLY < POST_IMAGES < CREATE_THREADS) and role-based overrides.
 */
@ExtendWith(MockitoExtension.class)
class FotoboxPermissionTest {

    @Mock private RoomModuleApi roomModule;
    @Mock private UserModuleApi userModule;
    @Mock private FotoboxRoomSettingsRepository settingsRepo;
    @Mock private FotoboxThreadRepository threadRepo;
    @Mock private FotoboxImageRepository imageRepo;

    private FotoboxPermissionService service;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ROOM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FotoboxPermissionService(roomModule, userModule, settingsRepo, threadRepo, imageRepo);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private UserInfo makeUser(UUID id, UserRole role) {
        return new UserInfo(
                id, id + "@monteweb.local", "Max", "Mustermann",
                "Max Mustermann", null, null,
                role, Set.of(), Set.of(), true, "SYSTEM"
        );
    }

    private FotoboxRoomSettings makeSettings(UUID roomId, boolean enabled, String defaultPermission) {
        var settings = new FotoboxRoomSettings();
        settings.setRoomId(roomId);
        settings.setEnabled(enabled);
        settings.setDefaultPermission(defaultPermission);
        return settings;
    }

    /**
     * Stubs common mocks for a non-admin room member with the given room role and default permission.
     */
    private void stubMember(UUID userId, UUID roomId, RoomRole roomRole, String defaultPermission) {
        when(userModule.findById(userId)).thenReturn(Optional.of(makeUser(userId, UserRole.PARENT)));
        when(roomModule.isUserInRoom(userId, roomId)).thenReturn(true);
        when(settingsRepo.findByRoomId(roomId))
                .thenReturn(Optional.of(makeSettings(roomId, true, defaultPermission)));
        when(roomModule.getUserRoleInRoom(userId, roomId)).thenReturn(Optional.of(roomRole));
    }

    // ── Permission Hierarchy ─────────────────────────────────────────────

    @Nested
    @DisplayName("Permission Hierarchy")
    class PermissionHierarchy {

        @Test
        @DisplayName("VIEW_ONLY member cannot post images")
        void viewOnlyCannotPostImages() {
            stubMember(USER_ID, ROOM_ID, RoomRole.MEMBER, "VIEW_ONLY");

            var level = service.getPermission(USER_ID, ROOM_ID);
            assertThat(level).isEqualTo(FotoboxPermissionLevel.VIEW_ONLY);

            assertThatThrownBy(() -> service.requirePermission(USER_ID, ROOM_ID, FotoboxPermissionLevel.POST_IMAGES))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Insufficient fotobox permissions");
        }

        @Test
        @DisplayName("POST_IMAGES member can post but cannot create threads")
        void postImagesCanPostButNotCreateThreads() {
            stubMember(USER_ID, ROOM_ID, RoomRole.MEMBER, "POST_IMAGES");

            var level = service.getPermission(USER_ID, ROOM_ID);
            assertThat(level).isEqualTo(FotoboxPermissionLevel.POST_IMAGES);

            // POST_IMAGES should pass for POST_IMAGES requirement
            assertThatCode(() -> service.requirePermission(USER_ID, ROOM_ID, FotoboxPermissionLevel.POST_IMAGES))
                    .doesNotThrowAnyException();

            // POST_IMAGES should fail for CREATE_THREADS requirement
            assertThatThrownBy(() -> service.requirePermission(USER_ID, ROOM_ID, FotoboxPermissionLevel.CREATE_THREADS))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Insufficient fotobox permissions");
        }

        @Test
        @DisplayName("LEADER always gets CREATE_THREADS regardless of default permission setting")
        void leaderAlwaysCreateThreads() {
            stubMember(USER_ID, ROOM_ID, RoomRole.LEADER, "VIEW_ONLY");

            var level = service.getPermission(USER_ID, ROOM_ID);
            assertThat(level).isEqualTo(FotoboxPermissionLevel.CREATE_THREADS);

            // LEADER should pass even the highest permission level
            assertThatCode(() -> service.requirePermission(USER_ID, ROOM_ID, FotoboxPermissionLevel.CREATE_THREADS))
                    .doesNotThrowAnyException();
        }
    }

    // ── Admin Role Consistency (SUPERADMIN + SECTION_ADMIN) ───────────────

    @Nested
    @DisplayName("Admin Role Consistency")
    class AdminRoleConsistency {

        @Test
        @DisplayName("SUPERADMIN is treated as admin even when not a room member")
        void superAdminNonMemberTreatedAsAdmin() {
            when(userModule.findById(USER_ID))
                    .thenReturn(Optional.of(makeUser(USER_ID, UserRole.SUPERADMIN)));

            assertThatCode(() -> service.requireRoomMember(USER_ID, ROOM_ID))
                    .doesNotThrowAnyException();
            assertThat(service.isLeaderOrAdmin(USER_ID, ROOM_ID)).isTrue();
        }

        @Test
        @DisplayName("SECTION_ADMIN is treated as admin even when not a room member")
        void sectionAdminNonMemberTreatedAsAdmin() {
            when(userModule.findById(USER_ID))
                    .thenReturn(Optional.of(makeUser(USER_ID, UserRole.SECTION_ADMIN)));

            // requireRoomMember must NOT throw for a non-member SECTION_ADMIN
            assertThatCode(() -> service.requireRoomMember(USER_ID, ROOM_ID))
                    .doesNotThrowAnyException();
            // SECTION_ADMIN must be recognised as leader-or-admin for moderation
            assertThat(service.isLeaderOrAdmin(USER_ID, ROOM_ID)).isTrue();
        }
    }
}
