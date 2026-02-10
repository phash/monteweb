package com.monteweb.room.internal.service;

import com.monteweb.messaging.ConversationInfo;
import com.monteweb.messaging.MessagingModuleApi;
import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.model.Room;
import com.monteweb.room.internal.model.RoomChatChannel;
import com.monteweb.room.internal.model.RoomChatChannel.ChannelType;
import com.monteweb.room.internal.model.RoomMember;
import com.monteweb.room.internal.repository.RoomChatChannelRepository;
import com.monteweb.shared.exception.BusinessException;
import com.monteweb.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages room-based group chat channels.
 * Each room can have up to 3 channels: MAIN (all members), PARENTS (parent members only),
 * STUDENTS (student members only).
 * Channels are backed by conversations from the messaging module.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "monteweb.modules.messaging", name = "enabled", havingValue = "true")
public class RoomChatService {

    private final RoomChatChannelRepository channelRepository;
    private final RoomService roomService;
    private final MessagingModuleApi messagingModuleApi;

    /**
     * Returns all chat channels for a room.
     */
    @Transactional(readOnly = true)
    public List<RoomChatChannelInfo> getChannels(UUID roomId, UUID userId) {
        // Verify user is a member
        if (!roomService.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("Not a member of this room");
        }

        List<RoomChatChannel> channels = channelRepository.findByRoomId(roomId);

        // Filter channels based on user's role
        RoomRole userRole = roomService.getUserRoleInRoom(userId, roomId).orElse(null);

        return channels.stream()
                .filter(ch -> canAccessChannel(ch.getChannelType(), userRole))
                .map(ch -> toChannelInfo(ch, userId))
                .toList();
    }

    /**
     * Gets or creates the MAIN channel for a room.
     */
    @Transactional
    public RoomChatChannelInfo getOrCreateMainChannel(UUID roomId, UUID userId) {
        return getOrCreateChannel(roomId, userId, ChannelType.MAIN);
    }

    /**
     * Gets or creates a specific channel type for a room.
     */
    @Transactional
    public RoomChatChannelInfo getOrCreateChannel(UUID roomId, UUID userId, ChannelType type) {
        if (!roomService.isUserInRoom(userId, roomId)) {
            throw new ForbiddenException("Not a member of this room");
        }

        Room room = roomService.findEntityById(roomId);
        if (!room.getSettings().chatEnabled()) {
            throw new BusinessException("Chat is not enabled for this room");
        }

        RoomRole userRole = roomService.getUserRoleInRoom(userId, roomId).orElse(null);
        if (!canAccessChannel(type, userRole)) {
            throw new ForbiddenException("You don't have access to this channel");
        }

        // Check if channel exists
        Optional<RoomChatChannel> existing = channelRepository.findByRoomIdAndChannelType(roomId, type);
        if (existing.isPresent()) {
            return toChannelInfo(existing.get(), userId);
        }

        // Only leaders can create parent/student channels
        if (type != ChannelType.MAIN && userRole != RoomRole.LEADER) {
            throw new ForbiddenException("Only room leaders can create specialized channels");
        }

        // Create conversation via messaging module - we'll store the channel mapping
        // The actual conversation creation will be handled when the first message is sent
        // For now, create a placeholder channel record
        RoomChatChannel channel = new RoomChatChannel();
        channel.setRoomId(roomId);
        channel.setChannelType(type);

        // We need a conversation ID - create a group conversation title
        String title = buildChannelTitle(room.getName(), type);
        // Use a deterministic UUID as placeholder until we can create via messaging API
        channel.setConversationId(UUID.randomUUID());

        channel = channelRepository.save(channel);
        return toChannelInfo(channel, userId);
    }

    private boolean canAccessChannel(ChannelType type, RoomRole role) {
        if (role == null) return false;
        return switch (type) {
            case MAIN -> true; // All members
            case PARENTS -> role == RoomRole.LEADER || role == RoomRole.PARENT_MEMBER;
            case STUDENTS -> role == RoomRole.LEADER || role == RoomRole.MEMBER;
        };
    }

    private String buildChannelTitle(String roomName, ChannelType type) {
        return switch (type) {
            case MAIN -> roomName + " - Chat";
            case PARENTS -> roomName + " - Eltern";
            case STUDENTS -> roomName + " - Schueler";
        };
    }

    private RoomChatChannelInfo toChannelInfo(RoomChatChannel channel, UUID userId) {
        // Try to get conversation info from messaging module
        Optional<ConversationInfo> convInfo = messagingModuleApi.findConversationById(
                channel.getConversationId(), userId);

        return new RoomChatChannelInfo(
                channel.getId(),
                channel.getRoomId(),
                channel.getConversationId(),
                channel.getChannelType().name(),
                convInfo.map(ConversationInfo::lastMessage).orElse(null),
                convInfo.map(ConversationInfo::unreadCount).orElse(0L)
        );
    }

    public record RoomChatChannelInfo(
            UUID id,
            UUID roomId,
            UUID conversationId,
            String channelType,
            String lastMessage,
            long unreadCount
    ) {}
}
