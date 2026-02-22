package com.monteweb.family;

import com.monteweb.family.internal.model.*;
import com.monteweb.family.internal.repository.FamilyInvitationRepository;
import com.monteweb.family.internal.repository.FamilyRepository;
import com.monteweb.family.internal.service.FamilyService;
import com.monteweb.family.internal.service.InviteCodeService;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FamilyService: family creation, leave, invite-code join,
 * and invitation acceptance/decline workflows.
 */
@ExtendWith(MockitoExtension.class)
class FamilyServiceTest {

    @Mock private FamilyRepository familyRepository;
    @Mock private FamilyInvitationRepository invitationRepository;
    @Mock private InviteCodeService inviteCodeService;
    @Mock private UserModuleApi userModuleApi;
    @Mock private ApplicationEventPublisher eventPublisher;

    private FamilyService service;

    private static final UUID FAMILY_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new FamilyService(
                familyRepository, invitationRepository, inviteCodeService,
                userModuleApi, eventPublisher
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Family makeFamily(UUID id, String name) {
        var family = new Family();
        family.setId(id);
        family.setName(name);
        family.setMembers(new ArrayList<>());
        return family;
    }

    private UserInfo makeUser(UUID id, UserRole role) {
        return new UserInfo(
                id, "test@monteweb.local", "Max", "Mustermann",
                "Max Mustermann", null, null,
                role, Set.of(), Set.of(), true
        );
    }

    private void stubFamilySave() {
        when(familyRepository.save(any(Family.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubUserLookup(UUID userId, UserRole role) {
        when(userModuleApi.findById(userId))
                .thenReturn(Optional.of(makeUser(userId, role)));
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Create Family
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Create Family")
    class CreateFamily {

        @Test
        @DisplayName("PARENT user creates family successfully")
        void create_parentSuccess() {
            stubUserLookup(USER_ID, UserRole.PARENT);
            when(familyRepository.findByMemberUserId(USER_ID)).thenReturn(List.of());
            stubFamilySave();

            var result = service.create("Familie Mueller", USER_ID);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Familie Mueller");
            verify(familyRepository, times(2)).save(any(Family.class));
        }

        @Test
        @DisplayName("STUDENT user is blocked from creating a family")
        void create_studentBlocked() {
            stubUserLookup(USER_ID, UserRole.STUDENT);

            assertThatThrownBy(() -> service.create("Familie Test", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Only parents and administrators");
        }

        @Test
        @DisplayName("User already in a family cannot create another")
        void create_alreadyInFamily() {
            stubUserLookup(USER_ID, UserRole.PARENT);
            var existingFamily = makeFamily(FAMILY_ID, "Existing Family");
            when(familyRepository.findByMemberUserId(USER_ID)).thenReturn(List.of(existingFamily));

            assertThatThrownBy(() -> service.create("Zweite Familie", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already belongs");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Leave Family
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Leave Family")
    class LeaveFamily {

        @Test
        @DisplayName("Last parent with children cannot leave")
        void leaveFamily_lastParentWithChildrenBlocked() {
            var family = makeFamily(FAMILY_ID, "Test Family");
            var parent = new FamilyMember(family, USER_ID, FamilyMemberRole.PARENT);
            var child = new FamilyMember(family, OTHER_USER_ID, FamilyMemberRole.CHILD);
            family.getMembers().add(parent);
            family.getMembers().add(child);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family));
            when(familyRepository.isMember(USER_ID, FAMILY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.leaveFamily(FAMILY_ID, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("last parent");
        }

        @Test
        @DisplayName("Parent leaves family that still has another parent")
        void leaveFamily_success() {
            var family = makeFamily(FAMILY_ID, "Test Family");
            var parent1 = new FamilyMember(family, USER_ID, FamilyMemberRole.PARENT);
            var parent2 = new FamilyMember(family, OTHER_USER_ID, FamilyMemberRole.PARENT);
            family.getMembers().add(parent1);
            family.getMembers().add(parent2);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family));
            when(familyRepository.isMember(USER_ID, FAMILY_ID)).thenReturn(true);

            service.leaveFamily(FAMILY_ID, USER_ID);

            assertThat(family.getMembers()).hasSize(1);
            assertThat(family.getMembers().get(0).getUserId()).isEqualTo(OTHER_USER_ID);
            verify(familyRepository).save(family);
            verify(familyRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Last member leaves — family is deleted")
        void leaveFamily_emptyFamilyDeleted() {
            var family = makeFamily(FAMILY_ID, "Test Family");
            var parent = new FamilyMember(family, USER_ID, FamilyMemberRole.PARENT);
            family.getMembers().add(parent);

            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family));
            when(familyRepository.isMember(USER_ID, FAMILY_ID)).thenReturn(true);

            service.leaveFamily(FAMILY_ID, USER_ID);

            assertThat(family.getMembers()).isEmpty();
            verify(invitationRepository).deleteByFamilyId(FAMILY_ID);
            verify(familyRepository).delete(family);
            verify(familyRepository, never()).save(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Join by Invite Code
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Join by Invite Code")
    class JoinByInviteCode {

        @Test
        @DisplayName("Valid invite code — user joins as PARENT")
        void joinByInviteCode_success() {
            var family = makeFamily(FAMILY_ID, "Test Family");
            family.setInviteCode("ABC123");
            family.setInviteExpires(Instant.now().plus(1, ChronoUnit.DAYS));

            stubUserLookup(USER_ID, UserRole.PARENT);
            when(familyRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(family));
            when(familyRepository.isMember(USER_ID, FAMILY_ID)).thenReturn(false);
            stubFamilySave();

            var result = service.joinByInviteCode("ABC123", USER_ID);

            assertThat(result).isNotNull();
            assertThat(family.getMembers()).hasSize(1);
            assertThat(family.getMembers().get(0).getRole()).isEqualTo(FamilyMemberRole.PARENT);
            verify(familyRepository).save(family);
        }

        @Test
        @DisplayName("Expired invite code is rejected")
        void joinByInviteCode_expired() {
            var family = makeFamily(FAMILY_ID, "Test Family");
            family.setInviteCode("EXPIRED");
            family.setInviteExpires(Instant.now().minus(1, ChronoUnit.DAYS));

            stubUserLookup(USER_ID, UserRole.PARENT);
            when(familyRepository.findByInviteCode("EXPIRED")).thenReturn(Optional.of(family));

            assertThatThrownBy(() -> service.joinByInviteCode("EXPIRED", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Already a member — cannot join again")
        void joinByInviteCode_alreadyMember() {
            var family = makeFamily(FAMILY_ID, "Test Family");
            family.setInviteCode("VALID");
            family.setInviteExpires(Instant.now().plus(1, ChronoUnit.DAYS));

            stubUserLookup(USER_ID, UserRole.PARENT);
            when(familyRepository.findByInviteCode("VALID")).thenReturn(Optional.of(family));
            when(familyRepository.isMember(USER_ID, FAMILY_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.joinByInviteCode("VALID", USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Already a member");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  Accept Invitation
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Accept Invitation")
    class AcceptInvitation {

        @Test
        @DisplayName("PENDING invitation accepted — member added and event published")
        void acceptInvitation_success() {
            UUID invitationId = UUID.randomUUID();
            UUID inviterId = UUID.randomUUID();

            var invitation = new FamilyInvitation(FAMILY_ID, inviterId, USER_ID, FamilyMemberRole.CHILD);
            invitation.setId(invitationId);
            invitation.setStatus(FamilyInvitationStatus.PENDING);

            var family = makeFamily(FAMILY_ID, "Test Family");

            // assertCanJoinFamily needs user lookup
            stubUserLookup(USER_ID, UserRole.STUDENT);
            // toInvitationInfo needs family name, inviter name, invitee name
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
            when(invitationRepository.save(any(FamilyInvitation.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(familyRepository.findById(FAMILY_ID)).thenReturn(Optional.of(family));
            when(familyRepository.isMember(USER_ID, FAMILY_ID)).thenReturn(false);
            stubFamilySave();
            // For toInvitationInfo, inviter lookup
            when(userModuleApi.findById(inviterId))
                    .thenReturn(Optional.of(makeUser(inviterId, UserRole.PARENT)));

            service.acceptInvitation(invitationId, USER_ID);

            assertThat(invitation.getStatus()).isEqualTo(FamilyInvitationStatus.ACCEPTED);
            assertThat(invitation.getResolvedAt()).isNotNull();
            assertThat(family.getMembers()).hasSize(1);
            assertThat(family.getMembers().get(0).getRole()).isEqualTo(FamilyMemberRole.CHILD);
            verify(eventPublisher).publishEvent(any(FamilyInvitationEvent.class));
        }

        @Test
        @DisplayName("Non-PENDING invitation cannot be accepted")
        void acceptInvitation_notPending() {
            UUID invitationId = UUID.randomUUID();

            var invitation = new FamilyInvitation(FAMILY_ID, OTHER_USER_ID, USER_ID, FamilyMemberRole.PARENT);
            invitation.setId(invitationId);
            invitation.setStatus(FamilyInvitationStatus.ACCEPTED);

            stubUserLookup(USER_ID, UserRole.PARENT);
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> service.acceptInvitation(invitationId, USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not pending");
        }

        @Test
        @DisplayName("Wrong user cannot accept someone else's invitation")
        void acceptInvitation_wrongUser() {
            UUID invitationId = UUID.randomUUID();
            UUID wrongUserId = UUID.randomUUID();

            var invitation = new FamilyInvitation(FAMILY_ID, OTHER_USER_ID, USER_ID, FamilyMemberRole.PARENT);
            invitation.setId(invitationId);
            invitation.setStatus(FamilyInvitationStatus.PENDING);

            stubUserLookup(wrongUserId, UserRole.PARENT);
            when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

            assertThatThrownBy(() -> service.acceptInvitation(invitationId, wrongUserId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Not your invitation");
        }
    }
}
