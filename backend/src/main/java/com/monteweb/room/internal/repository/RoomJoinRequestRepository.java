package com.monteweb.room.internal.repository;

import com.monteweb.room.internal.model.RoomJoinRequest;
import com.monteweb.room.internal.model.RoomJoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomJoinRequestRepository extends JpaRepository<RoomJoinRequest, UUID> {

    List<RoomJoinRequest> findByRoomIdAndStatus(UUID roomId, RoomJoinRequestStatus status);

    List<RoomJoinRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByRoomIdAndUserIdAndStatus(UUID roomId, UUID userId, RoomJoinRequestStatus status);
}
