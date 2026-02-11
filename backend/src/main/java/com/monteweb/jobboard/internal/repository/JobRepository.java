package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.JobStatus;
import com.monteweb.jobboard.internal.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Page<Job> findByStatusOrderByScheduledDateAscCreatedAtDesc(JobStatus status, Pageable pageable);

    Page<Job> findByStatusInOrderByScheduledDateAscCreatedAtDesc(List<JobStatus> statuses, Pageable pageable);

    Page<Job> findByCategoryAndStatusInOrderByScheduledDateAscCreatedAtDesc(
            String category, List<JobStatus> statuses, Pageable pageable);

    Page<Job> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

    Page<Job> findByEventIdAndStatusInOrderByScheduledDateAscCreatedAtDesc(
            UUID eventId, List<JobStatus> statuses, Pageable pageable);

    Page<Job> findByCategoryAndEventIdAndStatusInOrderByScheduledDateAscCreatedAtDesc(
            String category, UUID eventId, List<JobStatus> statuses, Pageable pageable);

    Page<Job> findByEventIdAndCategoryAndStatusInOrderByScheduledDateAscCreatedAtDesc(
            UUID eventId, String category, List<JobStatus> statuses, Pageable pageable);

    List<Job> findByEventId(UUID eventId);

    int countByEventId(UUID eventId);

    @Query("SELECT DISTINCT j.category FROM Job j ORDER BY j.category")
    List<String> findAllCategories();

    long countByStatus(JobStatus status);
}
