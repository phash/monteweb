package com.monteweb.room.internal.repository;

import com.monteweb.room.internal.model.RoomChatChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomChatChannelRepository extends JpaRepository<RoomChatChannel, UUID> {

    List<RoomChatChannel> findByRoomId(UUID roomId);

    Optional<RoomChatChannel> findByRoomIdAndChannelType(UUID roomId, RoomChatChannel.ChannelType channelType);

    Optional<RoomChatChannel> findByConversationId(UUID conversationId);

    boolean existsByRoomIdAndChannelType(UUID roomId, RoomChatChannel.ChannelType channelType);
}
