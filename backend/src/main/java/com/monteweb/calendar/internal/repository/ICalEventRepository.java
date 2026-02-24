package com.monteweb.calendar.internal.repository;

import com.monteweb.calendar.internal.model.ICalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICalEventRepository extends JpaRepository<ICalEvent, UUID> {

    Optional<ICalEvent> findBySubscriptionIdAndUid(UUID subscriptionId, String uid);

    List<ICalEvent> findByStartDateBetween(LocalDate from, LocalDate to);

    void deleteBySubscriptionId(UUID subscriptionId);
}
