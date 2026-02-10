package com.monteweb.room.internal.repository;

import com.monteweb.room.internal.model.Room;
import com.monteweb.room.internal.model.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    @Query("SELECT r FROM Room r JOIN r.members m WHERE m.userId = :userId AND r.archived = false")
    List<Room> findByMemberUserId(UUID userId);

    Page<Room> findByArchivedFalse(Pageable pageable);

    Page<Room> findBySectionIdAndArchivedFalse(UUID sectionId, Pageable pageable);

    List<Room> findByTypeAndArchivedFalse(RoomType type);

    // Interest rooms: discoverable and not archived
    @Query("SELECT r FROM Room r WHERE r.discoverable = true AND r.archived = false " +
            "ORDER BY r.name ASC")
    Page<Room> findDiscoverable(Pageable pageable);

    // Search interest rooms by name or tags
    @Query("SELECT r FROM Room r WHERE r.discoverable = true AND r.archived = false " +
            "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Room> searchDiscoverable(@Param("query") String query, Pageable pageable);

    // Find expired rooms for auto-archival
    @Query("SELECT r FROM Room r WHERE r.expiresAt IS NOT NULL AND r.expiresAt < :now " +
            "AND r.archived = false")
    List<Room> findExpiredRooms(@Param("now") Instant now);
}
