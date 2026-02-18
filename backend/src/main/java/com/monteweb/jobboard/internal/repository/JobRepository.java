package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.JobStatus;
import com.monteweb.jobboard.internal.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
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


    @Query("""
            SELECT j FROM Job j WHERE j.status IN :statuses
            AND (:category IS NULL OR j.category = :category)
            AND (:eventId IS NULL OR j.eventId = :eventId)
            AND (CAST(:fromDate AS date) IS NULL OR j.scheduledDate >= :fromDate)
            AND (CAST(:toDate AS date) IS NULL OR j.scheduledDate <= :toDate)
            ORDER BY j.scheduledDate ASC, j.createdAt DESC
            """)
    Page<Job> findWithFilters(List<JobStatus> statuses, String category, UUID eventId,
                              LocalDate fromDate, LocalDate toDate, Pageable pageable);

    List<Job> findByEventId(UUID eventId);

    int countByEventId(UUID eventId);

    @Query("SELECT DISTINCT j.category FROM Job j ORDER BY j.category")
    List<String> findAllCategories();

    long countByStatus(JobStatus status);
}
