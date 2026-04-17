package com.example.monew.domain.notification.repository;

import com.example.monew.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  // 단건 조회
  Optional<Notification> findByIdAndDeletedAtIsNull(UUID id);

  // 전체 알림 개수
  long countByUserIdAndDeletedAtIsNull(UUID userId);

  // 데이터 목록 반환
  List<Notification> findAllByUserIdAndConfirmedFalseAndDeletedAtIsNull(UUID userId);
  // 첫 페이지 조회
  @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findFirstPageByUserId(@Param("userId") UUID userId, Pageable pageable);

  // 다음 페이지 조회
  @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.deletedAt IS NULL " +
      "AND (n.createdAt < :after OR (n.createdAt = :after AND CAST(n.id AS string) < :cursor)) " +
      "ORDER BY n.createdAt DESC, n.id DESC")
  List<Notification> findNextPageByUserId(
      @Param("userId") UUID userId,
      @Param("after") LocalDateTime after,
      @Param("cursor") String cursor,
      Pageable pageable
  );
}