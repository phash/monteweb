package com.monteweb.admin.internal.repository;

import com.monteweb.admin.internal.model.ErrorReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ErrorReportRepository extends JpaRepository<ErrorReport, UUID> {
    Optional<ErrorReport> findByFingerprint(String fingerprint);

    Page<ErrorReport> findByStatus(String status, Pageable pageable);

    Page<ErrorReport> findBySource(String source, Pageable pageable);

    @Query("SELECT e FROM ErrorReport e WHERE (:status IS NULL OR e.status = :status) AND (:source IS NULL OR e.source = :source)")
    Page<ErrorReport> findFiltered(@Param("status") String status, @Param("source") String source, Pageable pageable);

    long countByStatus(String status);
}
