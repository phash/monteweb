package com.monteweb.admin.internal.repository;

import com.monteweb.admin.internal.model.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    Page<AuditLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLogEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Filtered audit-log query (AC US-348: "Filtern nach Zeitraum oder Aktion").
     * Any of the filter parameters may be null, in which case that filter is skipped.
     *
     * <p>The nullable parameters are CAST to their type inside the {@code IS NULL}
     * checks so PostgreSQL can determine the bind-parameter types even when the value
     * is null (otherwise: "could not determine data type of parameter").
     */
    @Query("""
            SELECT a FROM AuditLogEntry a
            WHERE (CAST(:action AS string) IS NULL OR a.action = :action)
              AND (CAST(:from AS timestamp) IS NULL OR a.createdAt >= :from)
              AND (CAST(:to AS timestamp) IS NULL OR a.createdAt <= :to)
            ORDER BY a.createdAt DESC
            """)
    Page<AuditLogEntry> findFiltered(@Param("action") String action,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}
