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

  // 수정: 본인 소유의 알림인지 확인하며 조회 (보안 강화)
  Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

  long countByUserIdAndDeletedAtIsNull(UUID userId);

  // 수정: 벌크 연산 최적화 및 명칭 변경
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE Notification n SET n.confirmed = true " +
      "WHERE n.userId = :userId AND n.confirmed = false AND n.deletedAt IS NULL")
  int confirmAllByUserId(@Param("userId") UUID userId);

  @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL " +
      "ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findFirstPageByUserId(@Param("userId") UUID userId, Pageable pageable);

  // 수정: CAST 제거 및 cursor 타입을 UUID로 변경 (성능 및 타입 안정성)
  @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL " +
      "AND (n.createdAt < :after OR (n.createdAt = :after AND n.id < :cursor)) " +
      "ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findNextPageByUserId(
      @Param("userId") UUID userId,
      @Param("after") LocalDateTime after,
      @Param("cursor") UUID cursor,
      Pageable pageable
  );
}