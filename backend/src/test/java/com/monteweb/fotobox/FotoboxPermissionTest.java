package com.monteweb.fotobox;

import com.monteweb.fotobox.internal.model.FotoboxRoomSettings;
import com.monteweb.fotobox.internal.repository.FotoboxImageRepository;
import com.monteweb.fotobox.internal.repository.FotoboxRoomSettingsRepository;
import com.monteweb.fotobox.internal.repository.FotoboxThreadRepository;
import com.monteweb.fotobox.internal.service.FotoboxPermissionService;
import com.monteweb.room.RoomInfo;
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
    private static final UUID SECTION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FotoboxPermissionService(roomModule, userModule, settingsRepo, threadRepo, imageRepo);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private UserInfo makeUser(UUID id, UserRole role) {
        return makeUser(id, role, Set.of());
    }

    private UserInfo makeUser(UUID id, UserRole role, Set<String> specialRoles) {
        return new UserInfo(
                id, id + "@monteweb.local", "Max", "Mustermann",
                "Max Mustermann", null, null,
                role, specialRoles, Set.of(), true, "SYSTEM"
        );
    }

    private RoomInfo makeRoom(UUID roomId, UUID sectionId) {
        return new RoomInfo(
                roomId, "Room", "desc", "pub", null, "CLASS",
                sectionId, false, 5, "OPEN", null, java.util.List.of(), null
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
        @DisplayName("SECTION_ADMIN scoped to the room's section is treated as admin (non-member)")
        void sectionAdminScopedToRoomSectionTreatedAsAdmin() {
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(
                    makeUser(USER_ID, UserRole.SECTION_ADMIN, Set.of("SECTION_ADMIN:" + SECTION_ID))));
            when(roomModule.findById(ROOM_ID)).thenReturn(Optional.of(makeRoom(ROOM_ID, SECTION_ID)));

            // requireRoomMember must NOT throw for a section-scoped SECTION_ADMIN of this room
            assertThatCode(() -> service.requireRoomMember(USER_ID, ROOM_ID))
                    .doesNotThrowAnyException();
            // SECTION_ADMIN of this room's section must be recognised as leader-or-admin
            assertThat(service.isLeaderOrAdmin(USER_ID, ROOM_ID)).isTrue();
        }

        @Test
        @DisplayName("SECTION_ADMIN scoped to a DIFFERENT section is NOT admin for this room")
        void sectionAdminScopedToOtherSectionNotAdmin() {
            UUID otherSection = UUID.randomUUID();
            when(userModule.findById(USER_ID)).thenReturn(Optional.of(
                    makeUser(USER_ID, UserRole.SECTION_ADMIN, Set.of("SECTION_ADMIN:" + otherSection))));
            when(roomModule.findById(ROOM_ID)).thenReturn(Optional.of(makeRoom(ROOM_ID, SECTION_ID)));
            // Not a room member either
            when(roomModule.isUserInRoom(USER_ID, ROOM_ID)).thenReturn(false);

            // requireRoomMember MUST throw: section admin of another section has no access
            assertThatThrownBy(() -> service.requireRoomMember(USER_ID, ROOM_ID))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Not a member of this room");
            // And must NOT be treated as leader-or-admin
            when(roomModule.getUserRoleInRoom(USER_ID, ROOM_ID)).thenReturn(Optional.empty());
            assertThat(service.isLeaderOrAdmin(USER_ID, ROOM_ID)).isFalse();
        }
    }
}
