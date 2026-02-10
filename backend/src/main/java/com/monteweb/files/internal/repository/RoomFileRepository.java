package com.monteweb.files.internal.repository;

import com.monteweb.files.internal.model.RoomFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RoomFileRepository extends JpaRepository<RoomFile, UUID> {

    List<RoomFile> findByRoomIdAndFolderIdIsNullOrderByCreatedAtDesc(UUID roomId);

    List<RoomFile> findByRoomIdAndFolderIdOrderByCreatedAtDesc(UUID roomId, UUID folderId);

    List<RoomFile> findByRoomIdOrderByCreatedAtDesc(UUID roomId);

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM RoomFile f WHERE f.roomId = :roomId")
    long sumFileSizeByRoomId(UUID roomId);

    void deleteAllByRoomId(UUID roomId);
}
