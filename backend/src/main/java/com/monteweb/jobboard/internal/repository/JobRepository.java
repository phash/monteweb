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

    @Query("SELECT DISTINCT j.category FROM Job j ORDER BY j.category")
    List<String> findAllCategories();

    long countByStatus(JobStatus status);
}
