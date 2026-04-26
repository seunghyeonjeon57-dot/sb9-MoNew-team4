package com.example.monew.domain.notification.service;

import com.example.monew.domain.notification.dto.CursorPageResponseNotificationDto;
import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.exception.NotificationException;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    UUID resultId = notificationService.createNotification(request);

    assertThat(resultId).isEqualTo(expectedId);
    verify(notificationRepository, times(1)).save(any(Notification.class));
  }

  @Test
  @DisplayName("특정 사용자의 알림 목록을 최신순으로 조회한다. (커서 페이징 첫 페이지)")
  void getNotifications_success() {
    UUID userId = UUID.randomUUID();
    int limit = 10;
    Notification mockNotification = mock(Notification.class);

    when(notificationRepository.findFirstPageByUserId(eq(userId), any(Pageable.class)))
        .thenReturn(List.of(mockNotification));

    when(notificationRepository.countByUserIdAndConfirmedFalseAndDeletedAtIsNull(userId))
        .thenReturn(1L);

    CursorPageResponseNotificationDto responseDto =
        notificationService.getNotifications(userId, null, null, limit);

    assertThat(responseDto.content()).hasSize(1);
    assertThat(responseDto.hasNext()).isFalse();
    verify(notificationRepository, times(1)).findFirstPageByUserId(eq(userId), any(Pageable.class));
  }

  @Test
  @DisplayName("알림 ID를 통해 특정 알림을 읽음(확인) 처리한다.")
  void readNotification_success() {
    UUID notificationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Notification mockNotification = mock(Notification.class);

    when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
        .thenReturn(Optional.of(mockNotification));

    notificationService.confirmNotification(notificationId, userId);

    verify(mockNotification, times(1)).confirm();
    verify(notificationRepository, times(1))
        .findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId);
  }

  @Test
  @DisplayName("여러 개의 알림을 한 번에 저장할 때 saveAll이 호출된다.")
  void createNotifications_Success() {
    List<NotificationRequest> requests = List.of(
        new NotificationRequest(UUID.randomUUID(), "알림1", ResourceType.COMMENT, UUID.randomUUID()),
        new NotificationRequest(UUID.randomUUID(), "알림2", ResourceType.COMMENT, UUID.randomUUID())
    );

    notificationService.createNotifications(requests);

    verify(notificationRepository, times(1)).saveAll(any());
  }

  @Test
  @DisplayName("특정 유저의 모든 알림을 읽음 처리 시 confirmAllByUserId가 호출된다.")
  void confirmAllNotifications_Success() {
    UUID userId = UUID.randomUUID();

    notificationService.confirmAllNotifications(userId);

    verify(notificationRepository, times(1)).confirmAllByUserId(userId);
  }

  @Test
  @DisplayName("잘못된 커서 값으로 알림 조회 시 예외가 발생한다.")
  void getNotifications_InvalidCursor() {
    UUID userId = UUID.randomUUID();
    String invalidCursor = "invalid-uuid-string";

    assertThatThrownBy(() -> notificationService.getNotifications(userId, invalidCursor, null, 10))
        .isInstanceOf(NotificationException.class);
  }

  @Test
  @DisplayName("cursor와 after가 모두 주어지고, 다음 페이지(hasNext)가 존재하는 경우 정상 조회된다.")
  void getNotifications_NextPage_Success() {
    UUID userId = UUID.randomUUID();
    String cursor = UUID.randomUUID().toString();
    java.time.LocalDateTime after = java.time.LocalDateTime.now();
    int limit = 2;

    Notification noti1 = mock(Notification.class);
    Notification noti2 = mock(Notification.class);
    Notification noti3 = mock(Notification.class);

    lenient().when(noti1.getId()).thenReturn(UUID.randomUUID());
    lenient().when(noti2.getId()).thenReturn(UUID.randomUUID());
    lenient().when(noti3.getId()).thenReturn(UUID.randomUUID());
    lenient().when(noti1.getCreatedAt()).thenReturn(after);
    lenient().when(noti2.getCreatedAt()).thenReturn(after);
    lenient().when(noti3.getCreatedAt()).thenReturn(after);

    when(notificationRepository.findNextPageByUserId(eq(userId), eq(after), any(UUID.class), any(Pageable.class)))
        .thenReturn(List.of(noti1, noti2, noti3));

    CursorPageResponseNotificationDto response =
        notificationService.getNotifications(userId, cursor, after, limit);

    assertThat(response.hasNext()).isTrue();
    assertThat(response.content()).hasSize(limit);
    verify(notificationRepository, times(1))
        .findNextPageByUserId(eq(userId), eq(after), any(UUID.class), any(Pageable.class));
  }

}
