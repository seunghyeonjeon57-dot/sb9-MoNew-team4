package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

  boolean existsByInterestIdAndUserId(UUID interestId, UUID userId);

  Optional<Subscription> findByInterestIdAndUserId(UUID interestId, UUID userId);

  List<Subscription> findAllByUserId(UUID userId);

  @Query("SELECT s.interestId FROM Subscription s WHERE s.userId = :userId")
  List<UUID> findInterestIdsByUserId(@Param("userId") UUID userId);

  @Query("SELECT s.interestId FROM Subscription s "
      + "WHERE s.userId = :userId AND s.interestId IN :interestIds")
  Set<UUID> findInterestIdsByUserIdAndInterestIdIn(
      @Param("userId") UUID userId,
      @Param("interestIds") Collection<UUID> interestIds);

  @Transactional
  long deleteAllByInterestId(UUID interestId);

  @Modifying
  @Transactional
  @Query("DELETE FROM Subscription s WHERE s.userId = :userId")
  long deleteAllByUserId(@Param("userId") UUID userId);
}
