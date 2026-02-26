package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.JobStatus;
import com.monteweb.jobboard.internal.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Finds a job by ID with a pessimistic write lock to prevent race conditions
     * during concurrent assignment operations.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM Job j WHERE j.id = :id")
    Optional<Job> findByIdForUpdate(UUID id);


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
            AND j.visibility = 'PUBLIC'
            AND (:category IS NULL OR j.category = :category)
            AND (:eventId IS NULL OR j.eventId = :eventId)
            AND (:roomId IS NULL OR j.roomId = :roomId)
            AND (CAST(:fromDate AS date) IS NULL OR j.scheduledDate >= :fromDate)
            AND (CAST(:toDate AS date) IS NULL OR j.scheduledDate <= :toDate)
            ORDER BY j.scheduledDate ASC, j.createdAt DESC
            """)
    Page<Job> findWithFilters(List<JobStatus> statuses, String category, UUID eventId, UUID roomId,
                              LocalDate fromDate, LocalDate toDate, Pageable pageable);

    @Query("""
            SELECT j FROM Job j WHERE j.visibility = 'DRAFT'
            AND j.status IN ('OPEN', 'ASSIGNED', 'IN_PROGRESS')
            ORDER BY j.createdAt DESC
            """)
    Page<Job> findDraftJobs(Pageable pageable);

    List<Job> findByEventId(UUID eventId);

    int countByEventId(UUID eventId);

    @Query("SELECT DISTINCT j.category FROM Job j ORDER BY j.category")
    List<String> findAllCategories();

    long countByStatus(JobStatus status);

    List<Job> findByCreatedBy(UUID createdBy);
}
