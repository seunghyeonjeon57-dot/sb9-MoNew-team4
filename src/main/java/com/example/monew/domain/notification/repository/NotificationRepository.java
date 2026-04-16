package com.example.monew.domain.notification.repository;

import com.example.monew.domain.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  List<Notification> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}