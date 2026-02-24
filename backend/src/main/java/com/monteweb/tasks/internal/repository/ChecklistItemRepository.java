package com.monteweb.tasks.internal.repository;

import com.monteweb.tasks.internal.model.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, UUID> {

    List<ChecklistItem> findByTaskIdOrderByPosition(UUID taskId);

    List<ChecklistItem> findByTaskIdIn(List<UUID> taskIds);

    void deleteByTaskId(UUID taskId);
}
