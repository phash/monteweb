package com.monteweb.tasks.internal.repository;

import com.monteweb.tasks.internal.model.TaskBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskBoardRepository extends JpaRepository<TaskBoard, UUID> {

    Optional<TaskBoard> findByRoomId(UUID roomId);

    boolean existsByRoomId(UUID roomId);
}
