package com.monteweb.room.internal.service;

import com.monteweb.room.RoomInfo;
import com.monteweb.room.RoomModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.model.Room;
import com.monteweb.room.internal.model.RoomMember;
import com.monteweb.room.internal.model.RoomSettings;
import com.monteweb.room.internal.model.RoomType;
import com.monteweb.room.internal.repository.RoomMemberRepository;
import com.monteweb.room.internal.repository.RoomRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RoomService implements RoomModuleApi {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository memberRepository;

    public RoomService(RoomRepository roomRepository, RoomMemberRepository memberRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
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
        room.setDiscoverable(true);
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
    public RoomInfo update(UUID roomId, String name, String description) {
        var room = findEntityById(roomId);
        if (name != null) room.setName(name);
        if (description != null) room.setDescription(description);
        return toRoomInfo(roomRepository.save(room));
    }

    @Transactional
    public RoomInfo updateInterestFields(UUID roomId, List<String> tags,
                                          Boolean discoverable, Instant expiresAt) {
        var room = findEntityById(roomId);
        if (tags != null) room.setTags(tags.toArray(new String[0]));
        if (discoverable != null) room.setDiscoverable(discoverable);
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
    public void addMember(UUID roomId, UUID userId, RoomRole role) {
        var room = findEntityById(roomId);
        if (memberRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new BusinessException("User is already a member of this room");
        }
        var member = new RoomMember(room, userId, role);
        room.getMembers().add(member);
        roomRepository.save(room);
    }

    @Transactional
    public void removeMember(UUID roomId, UUID userId) {
        var room = findEntityById(roomId);
        room.getMembers().removeIf(m -> m.getUserId().equals(userId));
        roomRepository.save(room);
    }

    @Transactional
    public void updateMemberRole(UUID roomId, UUID userId, RoomRole newRole) {
        var member = memberRepository.findByIdRoomIdAndIdUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Room member not found"));
        member.setRole(newRole);
        memberRepository.save(member);
    }

    /**
     * Join a discoverable room as MEMBER (self-service for interest rooms).
     */
    @Transactional
    public void joinRoom(UUID roomId, UUID userId) {
        var room = findEntityById(roomId);
        if (!room.isDiscoverable()) {
            throw new BusinessException("Room is not open for joining");
        }
        if (room.isArchived()) {
            throw new BusinessException("Room is archived");
        }
        if (memberRepository.existsByIdRoomIdAndIdUserId(roomId, userId)) {
            throw new BusinessException("Already a member of this room");
        }
        var member = new RoomMember(room, userId, RoomRole.MEMBER);
        room.getMembers().add(member);
        roomRepository.save(room);
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
    }

    public RoomSettings getSettings(UUID roomId) {
        return findEntityById(roomId).getSettings();
    }

    // --- Browse/Search for discoverable rooms ---

    public Page<RoomInfo> browseDiscoverable(Pageable pageable) {
        return roomRepository.findDiscoverable(pageable).map(this::toRoomInfo);
    }

    public Page<RoomInfo> searchDiscoverable(String query, Pageable pageable) {
        return roomRepository.searchDiscoverable(query, pageable).map(this::toRoomInfo);
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

    // --- Internal helpers ---

    Room findEntityById(UUID roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", roomId));
    }

    private RoomInfo toRoomInfo(Room room) {
        return new RoomInfo(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.getType().name(),
                room.getSectionId(),
                room.isArchived(),
                room.getMembers().size(),
                room.isDiscoverable(),
                room.getExpiresAt(),
                room.getTags() != null ? Arrays.asList(room.getTags()) : List.of()
        );
    }
}
