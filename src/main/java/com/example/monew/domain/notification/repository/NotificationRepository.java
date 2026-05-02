package com.example.monew.domain.notification.repository;

import com.example.monew.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

  long countByUserIdAndConfirmedFalseAndDeletedAtIsNull(UUID userId);

  // 벌크 연산 최적화 (확인 처리)
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Notification n SET n.confirmed = true, n.updatedAt = CURRENT_TIMESTAMP " +
      "WHERE n.userId = :userId AND n.confirmed = false AND n.deletedAt IS NULL")
  int confirmAllByUserId(@Param("userId") UUID userId);

  @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.confirmed = false AND n.deletedAt IS NULL " +
      "ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findFirstPageByUserId(@Param("userId") UUID userId, Pageable pageable);

  @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.confirmed = false AND n.deletedAt IS NULL " +
      "AND (n.createdAt < :after OR (n.createdAt = :after AND n.id < :cursor)) " +
      "ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findNextPageByUserId(
      @Param("userId") UUID userId,
      @Param("after") LocalDateTime after,
      @Param("cursor") UUID cursor,
      Pageable pageable
  );

  @Modifying(clearAutomatically = true)
  @Query("delete from Notification n where n.userId = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}
