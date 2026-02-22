package com.monteweb.user.internal.repository;

import com.monteweb.user.internal.model.DataAccessLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface DataAccessLogRepository extends JpaRepository<DataAccessLog, UUID> {

    Page<DataAccessLog> findByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId, Pageable pageable);

    Page<DataAccessLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    void deleteByCreatedAtBefore(Instant cutoff);
}
