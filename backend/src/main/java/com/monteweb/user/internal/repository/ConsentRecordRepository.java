package com.monteweb.user.internal.repository;

import com.monteweb.user.internal.model.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, UUID> {

    List<ConsentRecord> findByUserIdAndRevokedAtIsNull(UUID userId);

    Optional<ConsentRecord> findByUserIdAndConsentTypeAndRevokedAtIsNull(UUID userId, String consentType);

    List<ConsentRecord> findByUserId(UUID userId);
}
