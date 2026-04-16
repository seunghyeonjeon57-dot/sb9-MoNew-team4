package com.example.monew.domain.notification.service;

import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;

  @Transactional // 쓰기 작업에만 별도 선언
  public UUID createNotification(UUID userId, String content, ResourceType resourceType, UUID resourceId) {
    Notification notification = Notification.builder()
        .userId(userId)
        .content(content)
        .resourceType(resourceType)
        .resourceId(resourceId)
        .build();

    return notificationRepository.save(notification).getId();
  }
}