package com.monteweb.room.internal.repository;

import com.monteweb.room.internal.model.RoomSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomSubscriptionRepository extends JpaRepository<RoomSubscription, UUID> {

    Optional<RoomSubscription> findByUserIdAndRoomId(UUID userId, UUID roomId);

    @Query("SELECT s.roomId FROM RoomSubscription s WHERE s.userId = :userId AND s.feedMuted = true")
    List<UUID> findMutedRoomIdsByUserId(UUID userId);

    boolean existsByUserIdAndRoomIdAndFeedMuted(UUID userId, UUID roomId, boolean feedMuted);
}
