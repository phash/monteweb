package com.monteweb.admin.internal.service;

import com.monteweb.admin.internal.model.AuditLogEntry;
import com.monteweb.admin.internal.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void log(UUID userId, String action, String entityType, UUID entityId,
                    Map<String, Object> details, String ipAddress) {
        repository.save(new AuditLogEntry(userId, action, entityType, entityId, details, ipAddress));
    }

    public Page<AuditLogEntry> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * Returns audit-log entries optionally filtered by action and/or time range
     * (AC US-348: "Filtern nach Zeitraum oder Aktion"). Null filters are ignored.
     */
    public Page<AuditLogEntry> find(String action, Instant from, Instant to, Pageable pageable) {
        String normalizedAction = (action != null && !action.isBlank()) ? action : null;
        if (normalizedAction == null && from == null && to == null) {
            return repository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return repository.findFiltered(normalizedAction, from, to, pageable);
    }
}
