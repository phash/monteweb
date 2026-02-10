package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.AssignmentStatus;
import com.monteweb.jobboard.internal.model.JobAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobAssignmentRepository extends JpaRepository<JobAssignment, UUID> {

    List<JobAssignment> findByJobId(UUID jobId);

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
}
