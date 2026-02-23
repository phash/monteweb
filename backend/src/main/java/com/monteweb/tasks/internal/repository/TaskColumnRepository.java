package com.monteweb.tasks.internal.repository;

import com.monteweb.tasks.internal.model.TaskColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskColumnRepository extends JpaRepository<TaskColumn, UUID> {

    List<TaskColumn> findByBoardIdOrderByPosition(UUID boardId);

    int countByBoardId(UUID boardId);
}
