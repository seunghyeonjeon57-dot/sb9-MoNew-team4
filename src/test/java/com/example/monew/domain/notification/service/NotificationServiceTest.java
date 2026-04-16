package com.example.monew.domain.notification.service;

import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.dto.NotificationResponse;
import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationService notificationService;

  @Test
  @DisplayName("알림 생성 서비스 로직이 정상적으로 레포지토리를 호출하고 ID를 반환한다.")
  void createNotification_success() {
    // given
    UUID userId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();

    NotificationRequest request = new NotificationRequest(
        userId,
        "리팩터링 테스트 알림입니다.",
        ResourceType.COMMENT,
        resourceId
    );

    Notification savedNotification = mock(Notification.class);
    UUID expectedId = UUID.randomUUID();

    when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
    when(savedNotification.getId()).thenReturn(expectedId);

    // when
    UUID resultId = notificationService.createNotification(request);

    // then
    assertThat(resultId).isEqualTo(expectedId);
    verify(notificationRepository, times(1)).save(any(Notification.class));
  }

  @Test
  @DisplayName("특정 사용자의 알림 목록을 최신순으로 조회한다.")
  void getNotifications_success() {
    // given
    UUID userId = UUID.randomUUID();
    Notification mockNotification = mock(Notification.class);

    // Repository가 리스트를 반환하도록 설정
    when(notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(mockNotification));

    // when
    List<NotificationResponse> responses = notificationService.getNotifications(userId);

    // then
    assertThat(responses).hasSize(1);
    verify(notificationRepository, times(1)).findAllByUserIdOrderByCreatedAtDesc(userId);
  }

  @Test
  @DisplayName("알림 ID를 통해 특정 알림을 읽음 처리한다.")
  void readNotification_success() {
    // given
    UUID notificationId = UUID.randomUUID();
    Notification mockNotification = mock(Notification.class);

    when(notificationRepository.findById(notificationId))
        .thenReturn(Optional.of(mockNotification));

    // when
    notificationService.readNotification(notificationId);

    // then
    verify(mockNotification, times(1)).read();
  }
}