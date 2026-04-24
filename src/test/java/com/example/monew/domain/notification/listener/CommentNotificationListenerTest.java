package com.example.monew.domain.notification.listener;

import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentNotificationListenerTest {

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private CommentNotificationListener commentNotificationListener;

  @Test
  @DisplayName("댓글 좋아요 이벤트 수신 시 알림 생성 서비스 로직을 호출한다.")
  void handleCommentLikedEvent() {
    // Given
    CommentLikedEvent event = new CommentLikedEvent(
        UUID.randomUUID(), // receiverId
        UUID.randomUUID(), // commentId
        UUID.randomUUID(), // likerId
        "tester"           // likerNickname
    );

    // When
    commentNotificationListener.handleCommentLikedEvent(event);

    // Then
    verify(notificationService, times(1)).createNotification(any(NotificationRequest.class));
  }
}