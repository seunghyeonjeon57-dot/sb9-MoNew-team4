package com.example.monew.domain.notification.service;

import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.dto.NotificationResponse;
import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;

  @Transactional
  public UUID createNotification(NotificationRequest request) {
    Notification notification = Notification.builder()
        .userId(request.userId())
        .content(request.content())
        .resourceType(request.resourceType())
        .resourceId(request.resourceId())
        .build();

    return notificationRepository.save(notification).getId();
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> getNotifications(UUID userId) {
    return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(NotificationResponse::from)
        .collect(Collectors.toList());
  }

  @Transactional
  public void readNotification(UUID notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 알림입니다. ID: " + notificationId));

    notification.read();
  }
}