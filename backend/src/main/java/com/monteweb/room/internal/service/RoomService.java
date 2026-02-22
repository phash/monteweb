package com.monteweb.room.internal.service;

import com.monteweb.room.*;
import com.monteweb.room.internal.dto.JoinRequestInfo;
import com.monteweb.room.internal.model.*;
import com.monteweb.room.internal.repository.RoomChatChannelRepository;
import com.monteweb.room.internal.repository.RoomJoinRequestRepository;
import com.monteweb.room.internal.repository.RoomMemberRepository;
import com.monteweb.room.internal.repository.RoomRepository;
import com.monteweb.room.internal.repository.RoomSubscriptionRepository;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import com.monteweb.family.FamilyInfo;
import com.monteweb.family.FamilyModuleApi;
import com.monteweb.user.UserInfo;
import com.monteweb.user.UserModuleApi;
import com.monteweb.user.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoomService implements RoomModuleApi {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;
    private final RoomJoinRequestRepository joinRequestRepository;
    private final RoomSubscriptionRepository subscriptionRepository;
    private final UserModuleApi userModuleApi;
    private final FamilyModuleApi familyModuleApi;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomChatChannelRepository chatChannelRepository;
    private final MessagingModuleApi messagingModuleApi;

    public RoomService(RoomRepository roomRepository, RoomMemberRepository memberRepository,
                       RoomJoinRequestRepository joinRequestRepository,
                       RoomSubscriptionRepository subscriptionRepository,
                       UserModuleApi userModuleApi,
                       FamilyModuleApi familyModuleApi, ApplicationEventPublisher eventPublisher,
                       @Autowired(required = false) RoomChatChannelRepository chatChannelRepository,
                       @Autowired(required = false) MessagingModuleApi messagingModuleApi) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.userModuleApi = userModuleApi;
        this.familyModuleApi = familyModuleApi;
        this.eventPublisher = eventPublisher;
        this.chatChannelRepository = chatChannelRepository;
        this.messagingModuleApi = messagingModuleApi;
    }

    // --- Public API (RoomModuleApi) ---

    @Override
    public Optional<RoomInfo> findById(UUID roomId) {
        return roomRepository.findById(roomId).map(this::toRoomInfo);
    }

    @Override
    public List<RoomInfo> findByUserId(UUID userId) {
        return roomRepository.findByMemberUserId(userId).stream()
                .map(this::toRoomInfo)
                .toList();
    }

    @Override
    public boolean isUserInRoom(UUID userId, UUID roomId) {
        return memberRepository.existsByIdRoomIdAndIdUserId(roomId, userId);
    }

    @Override
    public Optional<RoomRole> getUserRoleInRoom(UUID userId, UUID roomId) {
        return memberRepository.findByIdRoomIdAndIdUserId(roomId, userId)
                .map(RoomMember::getRole);
    }

    @Override
    public List<UUID> getMemberUserIds(UUID roomId) {
        return memberRepository.findByIdRoomId(roomId).stream()
                .map(RoomMember::getUserId)
                .toList();
    }

    @Override
    public List<UUID> getMutedRoomIds(UUID userId) {
        return subscriptionRepository.findMutedRoomIdsByUserId(userId);
    }

    @Override
    public List<RoomInfo> findBySectionId(UUID sectionId) {
        return roomRepository.findBySectionIdAndArchivedFalse(sectionId,
                Pageable.unpaged()).stream().map(this::toRoomInfo).toList();
    }

    @Override
    @Transactional
    public RoomInfo createRoom(String name, String description, String type, UUID sectionId, UUID createdBy) {
        return create(name, description, RoomType.valueOf(type), sectionId, createdBy);
    }

    // --- Internal service methods ---

    public Page<RoomInfo> findAll(Pageable pageable) {
        return roomRepository.findByArchivedFalse(pageable).map(this::toRoomInfo);
    }

    public List<RoomInfo> findMyRooms(UUID userId) {
        return findByUserId(userId);
    }

    @Transactional
    public RoomInfo create(String name, String description, RoomType type, UUID sectionId, UUID createdBy) {
        var room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setType(type);
        room.setSectionId(sectionId);
        room.setCreatedBy(createdBy);
        room.setSettings(RoomSettings.defaults());

        room = roomRepository.save(room);

        // Creator becomes LEADER
        var member = new RoomMember(room, createdBy, RoomRole.LEADER);
        room.getMembers().add(member);
        room = roomRepository.save(room);

        eventPublisher.publishEvent(new RoomCreatedEvent(
                room.getId(), room.getName(), room.getType().name(), createdBy));

        return toRoomInfo(room);
    }

    /**
     * Creates an interest room (cross-section, discoverable, optional expiry).
     * Any authenticated user can create interest rooms.
     */
    @Transactional
    public RoomInfo createInterestRoom(String name, String description, List<String> tags,
                                        Instant expiresAt, UUID createdBy) {
        var room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setType(RoomType.INTEREST);
        room.setSectionId(null); // cross-section
        room.setCreatedBy(createdBy);
        room.setJoinPolicyEnum(JoinPolicy.OPEN);
        room.setTags(tags != null ? tags.toArray(new String[0]) : new String[0]);
        room.setExpiresAt(expiresAt);
        room.setSettings(RoomSettings.defaults());

        room = roomRepository.save(room);

        // Creator becomes LEADER (regardless of their role - anyone can lead interest rooms)
        var member = new RoomMember(room, createdBy, RoomRole.LEADER);
        room.getMembers().add(member);
        room = roomRepository.save(room);

        return toRoomInfo(room);
    }

    @Transactional
    public RoomInfo update(UUID roomId, String name, String description, String publicDescription,
                           RoomType type, UUID sectionId) {
        var room = findEntityById(roomId);
        if (name != null) room.setName(name);
        if (description != null) room.setDescription(description);
        if (publicDescription != null) room.setPublicDescription(publicDescription);
        if (type != null) room.setType(type);
        room.setSectionId(sectionId);
        return toRoomInfo(roomRepository.save(room));
    }

    @Transactional
    public void updateAvatarUrl(UUID roomId, String avatarUrl) {
        var room = findEntityById(roomId);
        room.setAvatarUrl(avatarUrl);
        roomRepository.save(room);
    }

    @Transactional
    public RoomInfo updateInterestFields(UUID roomId, List<String> tags,
                                          JoinPolicy joinPolicy, Instant expiresAt) {
        var room = findEntityById(roomId);
        if (tags != null) room.setTags(tags.toArray(new String[0]));
        if (joinPolicy != null) room.setJoinPolicyEnum(joinPolicy);
        if (expiresAt != null) room.setExpiresAt(expiresAt);
        return toRoomInfo(roomRepository.save(room));
    }

    @Transactional
    public RoomInfo updateSettings(UUID roomId, RoomSettings settings) {
        var room = findEntityById(roomId);
        room.setSettings(settings);
        return toRoomInfo(roomRepository.save(room));
    }

    @Transactional
    public void archive(UUID roomId) {
        var room = findEntityById(roomId);
        room.setArchived(true);
        roomRepository.save(room);
    }

    @Transactional
    public RoomInfo toggleArchive(UUID roomId) {
        var room = findEntityById(roomId);
        boolean newState = !room.isArchived();
        room.setArchived(newState);
        room.setArchiveAt(newState ? Instant.now() : null);
        return toRoomInfo(roomRepository.save(room));
    }

    @Transactional
    public void delete(UUID roomId) {
        var room = findEntityById(roomId);
        roomRepository.delete(room);
    }

    public Page<RoomInfo> findAllIncludingArchived(Pageable pageable) {
        return roomRepository.findAll(pageable).map(this::toRoomInfo);
    }

    @Transactional
    public void addMember(UUID roomId, UUID userId, RoomRole role) {
        var room = findEntityById(roomId);
        if (memberRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new BusinessException("User is already a member of this room");
        }
        // Auto-promote TEACHER to LEADER in KLASSE rooms
        RoomRole effectiveRole = resolveEffectiveRole(room, userId, role);
        var member = new RoomMember(room, userId, effectiveRole);
        room.getMembers().add(member);
        roomRepository.save(room);
        syncChatParticipantAdd(roomId, userId);
    }

    @Transactional
    public void removeMember(UUID roomId, UUID userId) {
        var room = findEntityById(roomId);
        room.getMembers().removeIf(m -> m.getUserId().equals(userId));
        roomRepository.save(room);
        syncChatParticipantRemove(roomId, userId);
    }

    @Transactional
    public void updateMemberRole(UUID roomId, UUID userId, RoomRole newRole) {
        var member = memberRepository.findByIdRoomIdAndIdUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Room member not found"));
        member.setRole(newRole);
        memberRepository.save(member);
    }

    @Transactional
    public int addFamilyMembers(UUID roomId, UUID familyId) {
        var room = findEntityById(roomId);
        var family = familyModuleApi.findById(familyId)
                .orElseThrow(() -> new ResourceNotFoundException("Family", familyId));

        int added = 0;
        for (var member : family.members()) {
            if (memberRepository.existsByIdRoomIdAndIdUserId(roomId, member.userId())) {
                continue; // already a member
            }
            RoomRole role = "PARENT".equals(member.role()) ? RoomRole.PARENT_MEMBER : RoomRole.MEMBER;
            room.getMembers().add(new RoomMember(room, member.userId(), role));
            added++;
        }
        if (added > 0) {
            roomRepository.save(room);
            // Sync all newly added members to chat conversations
            for (var member : family.members()) {
                syncChatParticipantAdd(roomId, member.userId());
            }
        }
        return added;
    }

    /**
     * Join an OPEN room as MEMBER (self-service).
     */
    @Transactional
    public void joinRoom(UUID roomId, UUID userId) {
        var room = findEntityById(roomId);
        if (room.getJoinPolicyEnum() != JoinPolicy.OPEN) {
            throw new BusinessException("Room is not open for joining");
        }
        if (room.isArchived()) {
            throw new BusinessException("Room is archived");
        }
        if (memberRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new BusinessException("Already a member of this room");
        }
        RoomRole effectiveRole = resolveEffectiveRole(room, userId, RoomRole.MEMBER);
        var member = new RoomMember(room, userId, effectiveRole);
        room.getMembers().add(member);
        roomRepository.save(room);
        syncChatParticipantAdd(roomId, userId);
    }

    /**
     * Leave a room (self-service). Leaders cannot leave if they're the only leader.
     */
    @Transactional
    public void leaveRoom(UUID roomId, UUID userId) {
        var member = memberRepository.findByIdRoomIdAndIdUserId(roomId, userId)
                .orElseThrow(() -> new BusinessException("Not a member of this room"));
        if (member.getRole() == RoomRole.LEADER) {
            var room = findEntityById(roomId);
            long leaderCount = room.getMembers().stream()
                    .filter(m -> m.getRole() == RoomRole.LEADER)
                    .count();
            if (leaderCount <= 1) {
                throw new BusinessException("Cannot leave: you are the only leader. Transfer leadership first.");
            }
        }
        var room = findEntityById(roomId);
        room.getMembers().removeIf(m -> m.getUserId().equals(userId));
        roomRepository.save(room);
        syncChatParticipantRemove(roomId, userId);
    }

    public RoomSettings getSettings(UUID roomId) {
        return findEntityById(roomId).getSettings();
    }

    // --- Browse/Search for open rooms ---

    public Page<RoomInfo> browseOpenRooms(Pageable pageable) {
        return roomRepository.findOpenRooms(pageable).map(this::toRoomInfo);
    }

    public Page<RoomInfo> searchOpenRooms(String query, Pageable pageable) {
        return roomRepository.searchOpenRooms(query, pageable).map(this::toRoomInfo);
    }

    // --- Scheduled auto-archival ---

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void archiveExpiredRooms() {
        List<Room> expired = roomRepository.findExpiredRooms(Instant.now());
        for (Room room : expired) {
            room.setArchived(true);
        }
        if (!expired.isEmpty()) {
            roomRepository.saveAll(expired);
        }
    }

    // --- Browse all rooms (not just discoverable) ---

    public Page<RoomInfo> browseAllRooms(UUID userId, Pageable pageable) {
        return roomRepository.findBrowsableRooms(userId, pageable).map(this::toRoomInfo);
    }

    public Page<RoomInfo> searchAllRooms(UUID userId, String query, Pageable pageable) {
        return roomRepository.searchBrowsableRooms(userId, query, pageable).map(this::toRoomInfo);
    }

    // --- Join requests ---

    @Transactional
    public JoinRequestInfo createJoinRequest(UUID roomId, UUID userId, String message) {
        var room = findEntityById(roomId);
        if (room.isArchived()) {
            throw new BusinessException("Room is archived");
        }
        if (memberRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new BusinessException("Already a member of this room");
        }
        if (joinRequestRepository.existsByRoomIdAndUserIdAndStatus(roomId, userId, RoomJoinRequestStatus.PENDING)) {
            throw new BusinessException("A pending request already exists");
        }

        var request = new RoomJoinRequest(roomId, userId, message);
        request = joinRequestRepository.save(request);

        String userName = userModuleApi.findById(userId).map(UserInfo::displayName).orElse("Unknown");
        eventPublisher.publishEvent(new RoomJoinRequestEvent(
                request.getId(), roomId, userId, userName, room.getName()));

        return toJoinRequestInfo(request);
    }

    public List<JoinRequestInfo> getPendingJoinRequests(UUID roomId) {
        return joinRequestRepository.findByRoomIdAndStatus(roomId, RoomJoinRequestStatus.PENDING).stream()
                .map(this::toJoinRequestInfo)
                .toList();
    }

    public List<JoinRequestInfo> getMyJoinRequests(UUID userId) {
        return joinRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toJoinRequestInfo)
                .toList();
    }

    @Transactional
    public JoinRequestInfo approveJoinRequest(UUID requestId, UUID resolvedBy) {
        var request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request", requestId));
        if (request.getStatus() != RoomJoinRequestStatus.PENDING) {
            throw new BusinessException("Request is not pending");
        }

        request.setStatus(RoomJoinRequestStatus.APPROVED);
        request.setResolvedBy(resolvedBy);
        request.setResolvedAt(Instant.now());
        joinRequestRepository.save(request);

        // Add user as member (auto-promote TEACHER to LEADER in KLASSE rooms)
        var room = findEntityById(request.getRoomId());
        if (!memberRepository.existsByIdRoomIdAndIdUserId(request.getRoomId(), request.getUserId())) {
            RoomRole effectiveRole = resolveEffectiveRole(room, request.getUserId(), RoomRole.MEMBER);
            var member = new RoomMember(room, request.getUserId(), effectiveRole);
            room.getMembers().add(member);
            roomRepository.save(room);
        }

        eventPublisher.publishEvent(new RoomJoinRequestResolvedEvent(
                requestId, request.getRoomId(), request.getUserId(), room.getName(), true));

        return toJoinRequestInfo(request);
    }

    @Transactional
    public JoinRequestInfo denyJoinRequest(UUID requestId, UUID resolvedBy) {
        var request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Join request", requestId));
        if (request.getStatus() != RoomJoinRequestStatus.PENDING) {
            throw new BusinessException("Request is not pending");
        }

        request.setStatus(RoomJoinRequestStatus.DENIED);
        request.setResolvedBy(resolvedBy);
        request.setResolvedAt(Instant.now());
        joinRequestRepository.save(request);

        var room = findEntityById(request.getRoomId());
        eventPublisher.publishEvent(new RoomJoinRequestResolvedEvent(
                requestId, request.getRoomId(), request.getUserId(), room.getName(), false));

        return toJoinRequestInfo(request);
    }

    private JoinRequestInfo toJoinRequestInfo(RoomJoinRequest request) {
        String userName = userModuleApi.findById(request.getUserId())
                .map(UserInfo::displayName).orElse("Unknown");
        String roomName = roomRepository.findById(request.getRoomId())
                .map(Room::getName).orElse("Unknown");
        return new JoinRequestInfo(
                request.getId(), request.getRoomId(), roomName,
                request.getUserId(), userName, request.getMessage(),
                request.getStatus().name(), request.getCreatedAt(), request.getResolvedAt());
    }

    // --- Feed subscription (mute/unmute) ---

    @Transactional
    public void muteRoom(UUID roomId, UUID userId) {
        findEntityById(roomId); // verify room exists
        var sub = subscriptionRepository.findByUserIdAndRoomId(userId, roomId)
                .orElseGet(() -> new RoomSubscription(userId, roomId));
        sub.setFeedMuted(true);
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void unmuteRoom(UUID roomId, UUID userId) {
        subscriptionRepository.findByUserIdAndRoomId(userId, roomId)
                .ifPresent(sub -> {
                    sub.setFeedMuted(false);
                    subscriptionRepository.save(sub);
                });
    }

    // --- Internal helpers ---

    /**
     * Auto-promote TEACHER to LEADER in KLASSE rooms.
     */
    private RoomRole resolveEffectiveRole(Room room, UUID userId, RoomRole requestedRole) {
        if (room.getType() == RoomType.KLASSE) {
            var user = userModuleApi.findById(userId);
            if (user.isPresent() && user.get().role() == UserRole.TEACHER) {
                return RoomRole.LEADER;
            }
        }
        return requestedRole;
    }

    Room findEntityById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
    }

    private RoomInfo toRoomInfo(Room room) {
        return new RoomInfo(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getPublicDescription(),
                room.getAvatarUrl(),
                room.getType().name(),
                room.getSectionId(),
                room.isArchived(),
                room.getMembers().size(),
                room.getJoinPolicy(),
                room.getExpiresAt(),
                room.getTags() != null ? Arrays.asList(room.getTags()) : List.of()
        );
    }

    // ---- Chat participant sync ----

    private void syncChatParticipantAdd(UUID roomId, UUID userId) {
        if (chatChannelRepository == null || messagingModuleApi == null) return;
        var channels = chatChannelRepository.findByRoomId(roomId);
        for (var channel : channels) {
            messagingModuleApi.addParticipantToConversation(channel.getConversationId(), userId);
        }
    }

    private void syncChatParticipantRemove(UUID roomId, UUID userId) {
        if (chatChannelRepository == null || messagingModuleApi == null) return;
        var channels = chatChannelRepository.findByRoomId(roomId);
        for (var channel : channels) {
            messagingModuleApi.removeParticipantFromConversation(channel.getConversationId(), userId);
        }
    }

    /**
     * DSGVO: Clean up all room data for a deleted user.
     */
    @Transactional
    public void cleanupUserData(UUID userId) {
        joinRequestRepository.deleteByUserId(userId);
        subscriptionRepository.deleteByUserId(userId);
        memberRepository.deleteByIdUserId(userId);
    }

    /**
     * DSGVO: Export all room data for a user.
     */
    @Override
    public Map<String, Object> exportUserData(UUID userId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        var memberships = memberRepository.findByIdUserId(userId);
        data.put("memberships", memberships.stream().map(m -> Map.of(
                "roomId", m.getId().getRoomId(),
                "role", m.getRole().name(),
                "joinedAt", m.getJoinedAt()
        )).toList());
        return data;
    }
}
