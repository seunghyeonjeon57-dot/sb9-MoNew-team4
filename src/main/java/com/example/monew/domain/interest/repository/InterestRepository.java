package com.example.monew.domain.interest.repository;

import com.example.monew.domain.interest.entity.Interest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterestRepository extends JpaRepository<Interest, UUID> {

  Optional<Interest> findByNameAndDeletedAtIsNull(String name);

  Optional<Interest> findByIdAndDeletedAtIsNull(UUID id);

  List<Interest> findAllByDeletedAtIsNull();

  @Modifying
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount + 1 WHERE i.id = :id")
  int incrementSubscriberCount(@Param("id") UUID id);

  @Modifying
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 WHERE i.id = :id AND i.subscriberCount > 0")
  int decrementSubscriberCount(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Interest i SET i.subscriberCount = i.subscriberCount - 1 "
      + "WHERE i.id IN :ids AND i.subscriberCount > 0")
  int decrementSubscriberCountAll(@Param("ids") Collection<UUID> ids);
}
