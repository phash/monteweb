package com.monteweb.tasks.internal.repository;

import com.monteweb.tasks.internal.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByBoardId(UUID boardId);

    List<Task> findByColumnIdOrderByPosition(UUID columnId);

    List<Task> findByAssigneeId(UUID assigneeId);

    List<Task> findByCreatedBy(UUID createdBy);

    int countByColumnId(UUID columnId);

    @Modifying
    @Query("UPDATE Task t SET t.assigneeId = null WHERE t.assigneeId = :userId")
    void anonymizeAssignee(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Task t SET t.createdBy = null WHERE t.createdBy = :userId")
    void anonymizeCreator(@Param("userId") UUID userId);
}
