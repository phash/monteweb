package com.monteweb.room;

import java.util.List;
import java.util.Map;
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

    /**
     * Returns all non-archived rooms belonging to a given section.
     */
    List<RoomInfo> findBySectionId(UUID sectionId);

    /**
     * Creates a new room and makes the given user its LEADER.
     * Used by section admin to create rooms within their sections.
     */
    RoomInfo createRoom(String name, String description, String type, UUID sectionId, UUID createdBy);

    /**
     * Searches non-archived rooms by name (case-insensitive LIKE).
     * Used by the global search module.
     */
    List<RoomInfo> searchRooms(String query, int limit);

    /**
     * DSGVO: Export all room-related data for a user.
     */
    Map<String, Object> exportUserData(UUID userId);
}
