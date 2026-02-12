package com.monteweb.room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API: Facade interface for the room module.
 * Other modules interact with rooms exclusively through this interface.
 */
public interface RoomModuleApi {

    Optional<RoomInfo> findById(UUID roomId);

    List<RoomInfo> findByUserId(UUID userId);

    boolean isUserInRoom(UUID userId, UUID roomId);

    Optional<RoomRole> getUserRoleInRoom(UUID userId, UUID roomId);

    /**
     * Returns all member user IDs of a room.
     */
    List<UUID> getMemberUserIds(UUID roomId);

    /**
     * Returns room IDs that the user has muted (feed posts from these rooms should be hidden).
     */
    List<UUID> getMutedRoomIds(UUID userId);
}
