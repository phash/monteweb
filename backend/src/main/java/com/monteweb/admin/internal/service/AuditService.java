package com.monteweb.admin.internal.service;

import com.monteweb.admin.internal.model.AuditLogEntry;
import com.monteweb.admin.internal.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
