package com.monteweb.files.internal.repository;

import com.monteweb.files.internal.model.RoomFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RoomFolderRepository extends JpaRepository<RoomFolder, UUID> {

    List<RoomFolder> findByRoomIdAndParentIdIsNullOrderByNameAsc(UUID roomId);

    List<RoomFolder> findByRoomIdAndParentIdOrderByNameAsc(UUID roomId, UUID parentId);

    List<RoomFolder> findByRoomIdOrderByNameAsc(UUID roomId);

    boolean existsByRoomIdAndParentIdAndName(UUID roomId, UUID parentId, String name);

    void deleteAllByRoomId(UUID roomId);
}
