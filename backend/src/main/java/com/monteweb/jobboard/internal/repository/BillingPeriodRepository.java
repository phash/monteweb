package com.monteweb.jobboard.internal.repository;

import com.monteweb.jobboard.internal.model.BillingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingPeriodRepository extends JpaRepository<BillingPeriod, UUID> {

    Optional<BillingPeriod> findByStatus(String status);

    List<BillingPeriod> findAllByOrderByStartDateDesc();
}
