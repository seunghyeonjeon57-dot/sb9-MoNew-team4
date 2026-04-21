package com.example.monew.domain.notification.repository;

import com.example.monew.domain.notification.entity.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Modifying(clearAutomatically = true) // 영속성 컨텍스트 초기화 필수
  @Query("delete from Notification n where n.userId = :userId")
  void deleteAllByUserId(@Param("userId") UUID userId);
}