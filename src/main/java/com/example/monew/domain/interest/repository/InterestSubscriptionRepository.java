package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.InterestSubscription;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestSubscriptionRepository
    extends JpaRepository<InterestSubscription, UUID> {

  boolean existsByInterestIdAndUserId(UUID interestId, UUID userId);

  long deleteByInterestIdAndUserId(UUID interestId, UUID userId);

  long deleteAllByInterestId(UUID interestId);

  @Query("""
      select s.interestId from InterestSubscription s
      where s.userId = :userId and s.interestId in :interestIds
      """)
  List<UUID> findInterestIdsByUserIdAndInterestIdIn(
      @Param("userId") UUID userId,
      @Param("interestIds") Collection<UUID> interestIds);
}
