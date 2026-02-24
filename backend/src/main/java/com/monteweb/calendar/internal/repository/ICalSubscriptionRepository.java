package com.monteweb.calendar.internal.repository;

import com.monteweb.calendar.internal.model.ICalSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ICalSubscriptionRepository extends JpaRepository<ICalSubscription, UUID> {

    List<ICalSubscription> findByActiveTrue();
}
