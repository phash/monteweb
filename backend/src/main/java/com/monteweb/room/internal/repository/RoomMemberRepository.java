package com.monteweb.room.internal.repository;

import com.monteweb.room.RoomRole;
import com.monteweb.room.internal.model.RoomMember;
import com.monteweb.room.internal.model.RoomMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMemberId> {

    List<RoomMember> findByIdRoomId(UUID roomId);

    Optional<RoomMember> findByIdRoomIdAndIdUserId(UUID roomId, UUID userId);

    boolean existsByIdRoomIdAndIdUserId(UUID roomId, UUID userId);

    List<RoomMember> findByIdRoomIdAndRole(UUID roomId, RoomRole role);
}
