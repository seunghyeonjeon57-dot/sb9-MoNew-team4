package com.example.monew.domain.notification.repository;

import com.example.monew.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
  // 지금은 기본 CRUD(save, findById 등)만 필요하므로 안은 비워둡니다!
}