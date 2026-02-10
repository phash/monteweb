package com.monteweb.admin.internal.repository;

import com.monteweb.admin.internal.model.AuditLogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, UUID> {

    Page<AuditLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLogEntry> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
