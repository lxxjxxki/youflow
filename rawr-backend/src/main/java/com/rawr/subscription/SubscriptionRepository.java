package com.rawr.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByEmail(String email);
    Optional<Subscription> findByUnsubscribeToken(String token);
}
