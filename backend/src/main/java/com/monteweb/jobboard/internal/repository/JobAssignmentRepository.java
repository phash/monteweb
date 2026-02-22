package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.AssignmentStatus;
import com.monteweb.jobboard.internal.model.JobAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobAssignmentRepository extends JpaRepository<JobAssignment, UUID> {

    List<JobAssignment> findByJobId(UUID jobId);

    void deleteByJobId(UUID jobId);

    List<JobAssignment> findByUserId(UUID userId);

    List<JobAssignment> findByFamilyId(UUID familyId);

    Optional<JobAssignment> findByJobIdAndUserId(UUID jobId, UUID userId);

    boolean existsByJobIdAndUserId(UUID jobId, UUID userId);

    long countByJobIdAndStatusNot(UUID jobId, AssignmentStatus status);

    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED'
            AND a.confirmed = true
            """)
    BigDecimal sumConfirmedHoursByFamilyId(UUID familyId);

    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED'
            AND a.confirmed = false
            """)
    BigDecimal sumPendingHoursByFamilyId(UUID familyId);

    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status IN ('ASSIGNED', 'IN_PROGRESS')
            """)
    BigDecimal sumInProgressHoursByFamilyId(UUID familyId);

    @Query("""
            SELECT DISTINCT a.familyId
            FROM JobAssignment a
            WHERE a.status = 'COMPLETED'
            """)
    List<UUID> findAllFamilyIdsWithAssignments();

    @Query("""
            SELECT a FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED'
            AND a.confirmed = true
            ORDER BY a.completedAt DESC
            """)
    List<JobAssignment> findConfirmedByFamilyId(UUID familyId);

    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED'
            AND a.confirmed = true
            AND a.confirmedAt >= :fromInstant
            AND a.confirmedAt < :toInstant
            """)
    BigDecimal sumConfirmedHoursByFamilyIdAndDateRange(UUID familyId, Instant fromInstant, Instant toInstant);

    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED'
            AND a.confirmed = false
            AND a.completedAt >= :fromInstant
            AND a.completedAt < :toInstant
            """)
    BigDecimal sumPendingHoursByFamilyIdAndDateRange(UUID familyId, Instant fromInstant, Instant toInstant);

    // Normal hours (all categories except Reinigung)
    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED' AND a.confirmed = true
            AND a.jobId NOT IN (SELECT j.id FROM Job j WHERE j.category = 'Reinigung')
            """)
    BigDecimal sumConfirmedNormalHoursByFamilyId(UUID familyId);

    // Reinigung hours from jobs
    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED' AND a.confirmed = true
            AND a.jobId IN (SELECT j.id FROM Job j WHERE j.category = 'Reinigung')
            """)
    BigDecimal sumConfirmedCleaningJobHoursByFamilyId(UUID familyId);

    // Normal hours in date range
    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED' AND a.confirmed = true
            AND a.confirmedAt >= :fromInstant AND a.confirmedAt < :toInstant
            AND a.jobId NOT IN (SELECT j.id FROM Job j WHERE j.category = 'Reinigung')
            """)
    BigDecimal sumConfirmedNormalHoursByFamilyIdAndDateRange(UUID familyId, Instant fromInstant, Instant toInstant);

    // Reinigung hours from jobs in date range
    @Query("""
            SELECT COALESCE(SUM(a.actualHours), 0)
            FROM JobAssignment a
            WHERE a.familyId = :familyId
            AND a.status = 'COMPLETED' AND a.confirmed = true
            AND a.confirmedAt >= :fromInstant AND a.confirmedAt < :toInstant
            AND a.jobId IN (SELECT j.id FROM Job j WHERE j.category = 'Reinigung')
            """)
    BigDecimal sumConfirmedCleaningJobHoursByFamilyIdAndDateRange(UUID familyId, Instant fromInstant, Instant toInstant);

    @Query("""
            SELECT a FROM JobAssignment a
            WHERE a.status = 'COMPLETED'
            AND a.confirmed = false
            ORDER BY a.completedAt ASC
            """)
    List<JobAssignment> findPendingConfirmation();

    void deleteByUserId(UUID userId);
}
