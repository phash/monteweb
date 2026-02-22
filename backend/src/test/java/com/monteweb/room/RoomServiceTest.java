package com.monteweb.room;

import com.monteweb.family.FamilyModuleApi;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.room.internal.model.*;
import com.monteweb.room.internal.repository.*;
import com.monteweb.room.internal.service.RoomService;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService Unit Tests")
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private RoomMemberRepository memberRepository;
    @Mock private RoomJoinRequestRepository joinRequestRepository;
    @Mock private RoomSubscriptionRepository subscriptionRepository;
    @Mock private UserModuleApi userModuleApi;
    @Mock private FamilyModuleApi familyModuleApi;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private RoomChatChannelRepository chatChannelRepository;
    @Mock private MessagingModuleApi messagingModuleApi;

    private RoomService roomService;

    // Shared test fixtures
    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID RESOLVER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        roomService = new RoomService(
                roomRepository, memberRepository, joinRequestRepository,
                subscriptionRepository, userModuleApi, familyModuleApi,
                eventPublisher, chatChannelRepository, messagingModuleApi
        );
    }

    // ---- Helpers ----

    private Room createRoom(RoomType type, String joinPolicy, boolean archived) {
        var room = new Room();
        room.setId(ROOM_ID);
        room.setName("Test Room");
        room.setType(type);
        room.setJoinPolicy(joinPolicy);
        room.setArchived(archived);
        room.setMembers(new ArrayList<>());
        return room;
    }

    private UserInfo createUserInfo(UUID userId, UserRole role) {
        return new UserInfo(
                userId, userId + "@test.local", "First", "Last",
                "First Last", null, null, role,
                Set.of(), Set.of(), true
        );
    }

    private RoomJoinRequest createJoinRequest(UUID requestId, RoomJoinRequestStatus status) {
        var request = new RoomJoinRequest(ROOM_ID, USER_ID, "Please let me join");
        request.setId(requestId);
        request.setStatus(status);
        request.setCreatedAt(Instant.now());
        return request;
    }

    private void stubRoomFound(Room room) {
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
    }

    private void stubNoChatChannels() {
        when(chatChannelRepository.findByRoomId(any())).thenReturn(List.of());
    }

    // ---- Join Room ----

    @Nested
    @DisplayName("joinRoom")
    class JoinRoom {

        @Test
        @DisplayName("joins an OPEN room as MEMBER successfully")
        void joinRoom_openRoomSuccess() {
            var room = createRoom(RoomType.GRUPPE, "OPEN", false);
            stubRoomFound(room);
            when(memberRepository.existsByIdRoomIdAndIdUserId(ROOM_ID, USER_ID)).thenReturn(false);
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
            stubNoChatChannels();

            roomService.joinRoom(ROOM_ID, USER_ID);

            assertThat(room.getMembers()).hasSize(1);
            assertThat(room.getMembers().get(0).getRole()).isEqualTo(RoomRole.MEMBER);
            assertThat(room.getMembers().get(0).getUserId()).isEqualTo(USER_ID);
            verify(roomRepository).save(room);
        }

        @Test
        @DisplayName("throws when room join policy is not OPEN")
        void joinRoom_notOpenThrows() {
            var room = createRoom(RoomType.GRUPPE, "REQUEST", false);
            stubRoomFound(room);

            assertThatThrownBy(() -> roomService.joinRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not open for joining");
        }

        @Test
        @DisplayName("throws when room is archived")
        void joinRoom_archivedThrows() {
            var room = createRoom(RoomType.GRUPPE, "OPEN", true);
            stubRoomFound(room);

            assertThatThrownBy(() -> roomService.joinRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("archived");
        }

        @Test
        @DisplayName("throws when user is already a member")
        void joinRoom_alreadyMember() {
            var room = createRoom(RoomType.GRUPPE, "OPEN", false);
            stubRoomFound(room);
            when(memberRepository.existsByIdRoomIdAndIdUserId(ROOM_ID, USER_ID)).thenReturn(true);

            assertThatThrownBy(() -> roomService.joinRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Already a member");
        }

        @Test
        @DisplayName("promotes TEACHER to LEADER in KLASSE room")
        void joinRoom_teacherInKlassePromotedToLeader() {
            var room = createRoom(RoomType.KLASSE, "OPEN", false);
            stubRoomFound(room);
            when(memberRepository.existsByIdRoomIdAndIdUserId(ROOM_ID, USER_ID)).thenReturn(false);
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(createUserInfo(USER_ID, UserRole.TEACHER)));
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
            stubNoChatChannels();

            roomService.joinRoom(ROOM_ID, USER_ID);

            assertThat(room.getMembers()).hasSize(1);
            assertThat(room.getMembers().get(0).getRole()).isEqualTo(RoomRole.LEADER);
        }
    }

    // ---- Leave Room ----

    @Nested
    @DisplayName("leaveRoom")
    class LeaveRoom {

        @Test
        @DisplayName("blocks last LEADER from leaving")
        void leaveRoom_lastLeaderBlocked() {
            var room = createRoom(RoomType.GRUPPE, "OPEN", false);
            var leader = new RoomMember(room, USER_ID, RoomRole.LEADER);
            room.getMembers().add(leader);

            when(memberRepository.findByIdRoomIdAndIdUserId(ROOM_ID, USER_ID))
                    .thenReturn(Optional.of(leader));
            stubRoomFound(room);

            assertThatThrownBy(() -> roomService.leaveRoom(ROOM_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("only leader");
        }

        @Test
        @DisplayName("MEMBER leaves successfully")
        void leaveRoom_memberSuccess() {
            var room = createRoom(RoomType.GRUPPE, "OPEN", false);
            var member = new RoomMember(room, USER_ID, RoomRole.MEMBER);
            room.getMembers().add(member);

            when(memberRepository.findByIdRoomIdAndIdUserId(ROOM_ID, USER_ID))
                    .thenReturn(Optional.of(member));
            stubRoomFound(room);
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
            stubNoChatChannels();

            roomService.leaveRoom(ROOM_ID, USER_ID);

            assertThat(room.getMembers()).isEmpty();
            verify(roomRepository).save(room);
        }
    }

    // ---- Approve Join Request ----

    @Nested
    @DisplayName("approveJoinRequest")
    class ApproveJoinRequest {

        @Test
        @DisplayName("approves PENDING request and adds member")
        void approveJoinRequest_successAddsMember() {
            var requestId = UUID.randomUUID();
            var request = createJoinRequest(requestId, RoomJoinRequestStatus.PENDING);
            var room = createRoom(RoomType.GRUPPE, "REQUEST", false);

            when(joinRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
            when(joinRequestRepository.save(any(RoomJoinRequest.class))).thenAnswer(inv -> inv.getArgument(0));
            stubRoomFound(room);
            when(memberRepository.existsByIdRoomIdAndIdUserId(ROOM_ID, USER_ID)).thenReturn(false);
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
            // toJoinRequestInfo needs user and room lookups
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(createUserInfo(USER_ID, UserRole.PARENT)));

            var result = roomService.approveJoinRequest(requestId, RESOLVER_ID);

            assertThat(request.getStatus()).isEqualTo(RoomJoinRequestStatus.APPROVED);
            assertThat(request.getResolvedBy()).isEqualTo(RESOLVER_ID);
            assertThat(request.getResolvedAt()).isNotNull();
            assertThat(room.getMembers()).hasSize(1);
            assertThat(result.status()).isEqualTo("APPROVED");
            verify(eventPublisher).publishEvent(any(RoomJoinRequestResolvedEvent.class));
        }

        @Test
        @DisplayName("throws when request is not PENDING")
        void approveJoinRequest_notPendingThrows() {
            var requestId = UUID.randomUUID();
            var request = createJoinRequest(requestId, RoomJoinRequestStatus.APPROVED);

            when(joinRequestRepository.findById(requestId)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> roomService.approveJoinRequest(requestId, RESOLVER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not pending");
        }
    }

    // ---- Resolve Effective Role (tested via joinRoom) ----

    @Nested
    @DisplayName("resolveEffectiveRole (via joinRoom)")
    class ResolveEffectiveRole {

        @Test
        @DisplayName("TEACHER in KLASSE room becomes LEADER")
        void resolveEffectiveRole_klasseTeacherBecomesLeader() {
            var room = createRoom(RoomType.KLASSE, "OPEN", false);
            stubRoomFound(room);
            when(memberRepository.existsByIdRoomIdAndIdUserId(ROOM_ID, USER_ID)).thenReturn(false);
            when(userModuleApi.findById(USER_ID)).thenReturn(Optional.of(createUserInfo(USER_ID, UserRole.TEACHER)));
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
            stubNoChatChannels();

            roomService.joinRoom(ROOM_ID, USER_ID);

            assertThat(room.getMembers()).hasSize(1);
            assertThat(room.getMembers().get(0).getRole()).isEqualTo(RoomRole.LEADER);
            verify(userModuleApi).findById(USER_ID);
        }

        @Test
        @DisplayName("TEACHER in non-KLASSE room stays MEMBER")
        void resolveEffectiveRole_nonKlasseUnchanged() {
            var room = createRoom(RoomType.GRUPPE, "OPEN", false);
            stubRoomFound(room);
            when(memberRepository.existsByIdRoomIdAndIdUserId(ROOM_ID, USER_ID)).thenReturn(false);
            when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
            stubNoChatChannels();

            roomService.joinRoom(ROOM_ID, USER_ID);

            assertThat(room.getMembers()).hasSize(1);
            assertThat(room.getMembers().get(0).getRole()).isEqualTo(RoomRole.MEMBER);
        }
    }
}
