package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByInterestIdAndUserId(UUID interestId, UUID userId);

  Optional<Subscription> findByInterestIdAndUserId(UUID interestId, UUID userId);

  List<Subscription> findAllByUserId(UUID userId);

  @Transactional
  long deleteAllByInterestId(UUID interestId);

  @Transactional
  long deleteAllByUserId(UUID userId);
}
