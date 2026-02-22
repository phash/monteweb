package com.monteweb.user.internal.repository;

import com.monteweb.user.internal.model.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {

    Optional<TermsAcceptance> findByUserIdAndTermsVersion(UUID userId, String termsVersion);

    boolean existsByUserIdAndTermsVersion(UUID userId, String termsVersion);
}
