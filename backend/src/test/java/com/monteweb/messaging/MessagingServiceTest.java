package com.monteweb.messaging;

import com.monteweb.admin.AdminModuleApi;
import com.monteweb.admin.TenantConfigInfo;
import com.monteweb.messaging.internal.model.*;
import com.monteweb.messaging.internal.repository.*;
import com.monteweb.messaging.internal.service.MessagingService;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessagingService covering direct conversations, communication rules,
 * message sending, and conversation muting.
 */
@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private MessageImageRepository messageImageRepository;
    @Mock private MessageReactionRepository messageReactionRepository;
    @Mock private MessagePollRepository messagePollRepository;
    @Mock private MessagePollVoteRepository messagePollVoteRepository;
    @Mock private UserModuleApi userModuleApi;
    @Mock private AdminModuleApi adminModuleApi;
    @Mock private MessageChannel messageChannel;
    private SimpMessagingTemplate messagingTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;

    private MessagingService service;

    private static final UUID USER_A = UUID.randomUUID();
    private static final UUID USER_B = UUID.randomUUID();
    private static final UUID CONV_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(messageChannel.send(any(org.springframework.messaging.Message.class))).thenReturn(true);
        lenient().when(messageChannel.send(any(org.springframework.messaging.Message.class), anyLong())).thenReturn(true);
        messagingTemplate = new SimpMessagingTemplate(messageChannel);
        service = new MessagingService(
                conversationRepository, participantRepository,
                messageRepository, messageImageRepository,
                messageReactionRepository, messagePollRepository, messagePollVoteRepository,
                userModuleApi, adminModuleApi,
                messagingTemplate, eventPublisher, null
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private UserInfo makeUser(UUID id, UserRole role) {
        return new UserInfo(
                id, id + "@monteweb.local", "Max", "Mustermann",
                "Max Mustermann", null, null,
                role, Set.of(), Set.of(), true, "SYSTEM"
        );
    }

    private TenantConfigInfo makeTenantConfig(boolean parentToParent, boolean studentToStudent) {
        return new TenantConfigInfo(
                UUID.randomUUID(), "TestSchool", null,
                Map.of(), Map.of(),
                BigDecimal.TEN, BigDecimal.ONE,
                parentToParent, studentToStudent,
                "BY", List.of(),
                null, false, true,
                true, "de", List.of("de", "en"), true,
                null, null, null, null,
                null, null, null, null,
                null, null, null,
                "DISABLED", null,
                false,
                // LDAP fields (enabled via modules map)
                null, null, null,
                null, null, null, null,
                "PARENT", false, false,
                // Maintenance (enabled via modules)
                null,
                // ClamAV (enabled via modules)
                null, 3310,
                // Jitsi (enabled via modules)
                null,
                // WOPI (enabled via modules)
                null
        );
    }

    private Conversation makeConversation(UUID id, boolean isGroup, UUID createdBy) {
        var conv = new Conversation();
        conv.setId(id);
        conv.setGroup(isGroup);
        conv.setCreatedBy(createdBy);
        conv.setCreatedAt(Instant.now());
        conv.setUpdatedAt(Instant.now());
        return conv;
    }

    private ConversationParticipant makeParticipant(UUID conversationId, UUID userId) {
        var p = new ConversationParticipant();
        p.setConversationId(conversationId);
        p.setUserId(userId);
        p.setJoinedAt(Instant.now());
        p.setMuted(false);
        return p;
    }

    private Message makeMessage(UUID id, UUID conversationId, UUID senderId, String content) {
        var msg = new Message();
        msg.setId(id);
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(content);
        msg.setCreatedAt(Instant.now());
        return msg;
    }

    /**
     * Stubs the repository calls that toConversationInfo needs internally.
     * Does NOT stub userModuleApi.findById — callers must stub that themselves
     * (to avoid overriding role-specific stubs needed by enforceCommRules).
     */
    private void stubToConversationInfo(UUID convId, UUID currentUserId, UUID otherUserId) {
        var participants = List.of(
                makeParticipant(convId, currentUserId),
                makeParticipant(convId, otherUserId)
        );
        lenient().when(participantRepository.findByConversationId(convId)).thenReturn(participants);
        lenient().when(messageRepository.findFirstByConversationIdOrderByCreatedAtDesc(convId))
                .thenReturn(Optional.empty());
        lenient().when(messageRepository.countUnreadMessages(eq(convId), eq(currentUserId), any(Instant.class)))
                .thenReturn(0L);
    }

    // ── Start Direct Conversation ────────────────────────────────────────

    @Nested
    @DisplayName("Start Direct Conversation")
    class StartDirectConversation {

        @Test
        @DisplayName("Self-messaging is blocked")
        void startDirectConversation_selfMessagingBlocked() {
            assertThatThrownBy(() -> service.startDirectConversation(USER_A, USER_A))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("yourself");
        }

        @Test
        @DisplayName("Returns existing conversation when one already exists")
        void startDirectConversation_existingReturnsExisting() {
            // Both users are staff so comm rules pass
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.TEACHER)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.PARENT)));

            var existing = makeConversation(CONV_ID, false, USER_A);
            when(conversationRepository.findDirectConversation(USER_A, USER_B))
                    .thenReturn(List.of(existing));

            // Stub toConversationInfo
            stubToConversationInfo(CONV_ID, USER_A, USER_B);

            var result = service.startDirectConversation(USER_A, USER_B);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CONV_ID);
            verify(conversationRepository, never()).save(any(Conversation.class));
        }

        @Test
        @DisplayName("Creates new conversation when none exists")
        void startDirectConversation_newSuccess() {
            // Both users are staff so comm rules pass
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.TEACHER)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.PARENT)));

            when(conversationRepository.findDirectConversation(USER_A, USER_B))
                    .thenReturn(List.of());

            var savedConv = makeConversation(CONV_ID, false, USER_A);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);

            // Stub toConversationInfo
            stubToConversationInfo(CONV_ID, USER_A, USER_B);

            var result = service.startDirectConversation(USER_A, USER_B);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(CONV_ID);
            verify(conversationRepository).save(any(Conversation.class));
            verify(participantRepository, times(2)).save(any(ConversationParticipant.class));
        }
    }

    // ── Communication Rules ──────────────────────────────────────────────

    @Nested
    @DisplayName("Communication Rules")
    class CommunicationRules {

        @Test
        @DisplayName("Staff role bypasses all comm rules")
        void commRules_staffBypass() {
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.TEACHER)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.PARENT)));

            when(conversationRepository.findDirectConversation(USER_A, USER_B)).thenReturn(List.of());

            var savedConv = makeConversation(CONV_ID, false, USER_A);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);
            stubToConversationInfo(CONV_ID, USER_A, USER_B);

            // Should not throw — teacher can always message anyone
            assertThatCode(() -> service.startDirectConversation(USER_A, USER_B))
                    .doesNotThrowAnyException();

            verify(adminModuleApi, never()).getTenantConfig();
        }

        @Test
        @DisplayName("Parent-to-parent allowed when config enabled")
        void commRules_parentToParentEnabled() {
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.PARENT)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.PARENT)));
            when(adminModuleApi.getTenantConfig()).thenReturn(makeTenantConfig(true, false));

            when(conversationRepository.findDirectConversation(USER_A, USER_B)).thenReturn(List.of());

            var savedConv = makeConversation(CONV_ID, false, USER_A);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);
            stubToConversationInfo(CONV_ID, USER_A, USER_B);

            assertThatCode(() -> service.startDirectConversation(USER_A, USER_B))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Parent-to-parent blocked when config disabled")
        void commRules_parentToParentDisabled() {
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.PARENT)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.PARENT)));
            when(adminModuleApi.getTenantConfig()).thenReturn(makeTenantConfig(false, false));

            assertThatThrownBy(() -> service.startDirectConversation(USER_A, USER_B))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Parent-to-parent");
        }

        @Test
        @DisplayName("Student-to-student allowed when config enabled")
        void commRules_studentToStudentEnabled() {
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.STUDENT)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.STUDENT)));
            when(adminModuleApi.getTenantConfig()).thenReturn(makeTenantConfig(false, true));

            when(conversationRepository.findDirectConversation(USER_A, USER_B)).thenReturn(List.of());

            var savedConv = makeConversation(CONV_ID, false, USER_A);
            when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConv);
            stubToConversationInfo(CONV_ID, USER_A, USER_B);

            assertThatCode(() -> service.startDirectConversation(USER_A, USER_B))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Student-to-student blocked when config disabled")
        void commRules_studentToStudentDisabled() {
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.STUDENT)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.STUDENT)));
            when(adminModuleApi.getTenantConfig()).thenReturn(makeTenantConfig(false, false));

            assertThatThrownBy(() -> service.startDirectConversation(USER_A, USER_B))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("Student-to-student");
        }

        @Test
        @DisplayName("Cross-role (parent-student) is always blocked")
        void commRules_crossRoleBlocked() {
            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.PARENT)));
            when(userModuleApi.findById(USER_B)).thenReturn(Optional.of(makeUser(USER_B, UserRole.STUDENT)));
            when(adminModuleApi.getTenantConfig()).thenReturn(makeTenantConfig(true, true));

            assertThatThrownBy(() -> service.startDirectConversation(USER_A, USER_B))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("not allowed");
        }
    }

    // ── Send Message ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Send Message")
    class SendMessage {

        @Test
        @DisplayName("Throws when no content and no image provided")
        void sendMessage_noContentNoImageThrows() {
            when(participantRepository.existsByConversationIdAndUserId(CONV_ID, USER_A)).thenReturn(true);

            assertThatThrownBy(() -> service.sendMessage(CONV_ID, USER_A, null, null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("text content or an image");
        }

        @Test
        @DisplayName("Throws when reply-to message does not exist")
        void sendMessage_invalidReplyTo() {
            UUID fakeReplyId = UUID.randomUUID();

            when(participantRepository.existsByConversationIdAndUserId(CONV_ID, USER_A)).thenReturn(true);
            when(messageRepository.existsById(fakeReplyId)).thenReturn(false);

            assertThatThrownBy(() -> service.sendMessage(CONV_ID, USER_A, "Hello", fakeReplyId, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Reply-to message not found");
        }

        @Test
        @DisplayName("Sends text message successfully")
        void sendMessage_textSuccess() {
            UUID msgId = UUID.randomUUID();

            when(participantRepository.existsByConversationIdAndUserId(CONV_ID, USER_A)).thenReturn(true);

            var savedMsg = makeMessage(msgId, CONV_ID, USER_A, "Hello World");
            when(messageRepository.save(any(Message.class))).thenReturn(savedMsg);

            var conv = makeConversation(CONV_ID, false, USER_A);
            when(conversationRepository.findById(CONV_ID)).thenReturn(Optional.of(conv));
            when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);

            var participants = List.of(
                    makeParticipant(CONV_ID, USER_A),
                    makeParticipant(CONV_ID, USER_B)
            );
            when(participantRepository.findByConversationId(CONV_ID)).thenReturn(participants);

            when(userModuleApi.findById(USER_A)).thenReturn(Optional.of(makeUser(USER_A, UserRole.PARENT)));

            var result = service.sendMessage(CONV_ID, USER_A, "Hello World", null, null);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(msgId);
            assertThat(result.content()).isEqualTo("Hello World");
            assertThat(result.senderId()).isEqualTo(USER_A);

            verify(messageRepository).save(any(Message.class));
            verify(participantRepository).markAsRead(eq(CONV_ID), eq(USER_A), any(Instant.class));
            verify(eventPublisher).publishEvent(any(MessageSentEvent.class));
        }
    }

    // ── Mute Conversation ────────────────────────────────────────────────

    @Nested
    @DisplayName("Mute Conversation")
    class MuteConversation {

        @Test
        @DisplayName("Mutes conversation for participant")
        void muteConversation_success() {
            when(participantRepository.existsByConversationIdAndUserId(CONV_ID, USER_A)).thenReturn(true);

            var participant = makeParticipant(CONV_ID, USER_A);
            when(participantRepository.findById(any(ConversationParticipant.ConversationParticipantId.class)))
                    .thenReturn(Optional.of(participant));

            service.muteConversation(CONV_ID, USER_A);

            assertThat(participant.isMuted()).isTrue();
            verify(participantRepository).save(participant);
        }

        @Test
        @DisplayName("Non-participant cannot mute conversation")
        void muteConversation_nonParticipantThrows() {
            when(participantRepository.existsByConversationIdAndUserId(CONV_ID, USER_A)).thenReturn(false);

            assertThatThrownBy(() -> service.muteConversation(CONV_ID, USER_A))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("not a participant");
        }
    }
}
