package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository
    extends JpaRepository<Interest, UUID>, JpaSpecificationExecutor<Interest> {

  List<Interest> findAllByIsDeletedFalse();

  boolean existsByNameAndIsDeletedFalse(String name);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update Interest i set i.subscriberCount = i.subscriberCount + 1 where i.id = :id")
  int incrementSubscriberCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Interest i set i.subscriberCount = i.subscriberCount - 1
      where i.id = :id and i.subscriberCount > 0
      """)
  int decrementSubscriberCount(@Param("id") UUID id);
}
