package com.example.webhook.repository;

import com.example.webhook.model.Subscription;
import com.example.webhook.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, String> {
    List<Subscription> findByClientId(String clientId);

    List<Subscription> findByEventTypeIdAndStatus(String eventTypeId, SubscriptionStatus status);

    List<Subscription> findByClientIdAndStatus(String clientId, SubscriptionStatus status);

    List<Subscription> findByClientIdAndEventTypeId(String clientId, String eventTypeId);

    List<Subscription> findByStatus(SubscriptionStatus status);
}
