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

    // Open rooms: joinPolicy = OPEN and not archived
    @Query("SELECT r FROM Room r WHERE r.joinPolicy = 'OPEN' AND r.archived = false " +
            "ORDER BY r.name ASC")
    Page<Room> findOpenRooms(Pageable pageable);

    // Search open rooms by name or description
    @Query("SELECT r FROM Room r WHERE r.joinPolicy = 'OPEN' AND r.archived = false " +
            "AND (LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Room> searchOpenRooms(@Param("query") String query, Pageable pageable);

    // Find expired rooms for auto-archival
    @Query("SELECT r FROM Room r WHERE r.expiresAt IS NOT NULL AND r.expiresAt < :now " +
            "AND r.archived = false")
    List<Room> findExpiredRooms(@Param("now") Instant now);

    // Browse all non-archived rooms where user is NOT a member
    @Query("SELECT r FROM Room r WHERE r.archived = false " +
            "AND r.id NOT IN (SELECT m.id.roomId FROM RoomMember m WHERE m.userId = :userId) " +
            "ORDER BY r.name ASC")
    Page<Room> findBrowsableRooms(@Param("userId") UUID userId, Pageable pageable);

    // Search browsable rooms by name
    @Query("SELECT r FROM Room r WHERE r.archived = false " +
            "AND r.id NOT IN (SELECT m.id.roomId FROM RoomMember m WHERE m.userId = :userId) " +
            "AND LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY r.name ASC")
    Page<Room> searchBrowsableRooms(@Param("userId") UUID userId, @Param("query") String query, Pageable pageable);

    // Global search: search all non-archived rooms by name
    @Query("SELECT r FROM Room r WHERE r.archived = false " +
            "AND LOWER(r.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "ORDER BY r.name ASC")
    List<Room> searchByName(@Param("query") String query, Pageable pageable);
}
