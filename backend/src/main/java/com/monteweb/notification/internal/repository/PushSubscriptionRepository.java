package com.monteweb.notification.internal.repository;

import com.monteweb.notification.internal.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    List<PushSubscription> findByUserId(UUID userId);

    Optional<PushSubscription> findByUserIdAndEndpoint(UUID userId, String endpoint);

    void deleteByUserIdAndEndpoint(UUID userId, String endpoint);

    void deleteByUserId(UUID userId);
}
