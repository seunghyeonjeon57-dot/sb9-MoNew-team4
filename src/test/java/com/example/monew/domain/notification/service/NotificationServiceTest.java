package com.example.monew.domain.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  @DisplayName("알림 생성 시 Repository의 save 메서드가 호출되어야 한다. (RED)")
  void createNotification() {
    // given (준비)
    Long userId = 1L;
    String content = "새로운 알림입니다.";
    ResourceType type = ResourceType.COMMENT;
    Long resourceId = 20L;

    // when (실행)
    notificationService.createNotification(userId, content, type, resourceId);

    // then (검증)
    verify(notificationRepository).save(any(Notification.class));
  }
}