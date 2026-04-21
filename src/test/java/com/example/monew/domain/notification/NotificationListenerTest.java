package com.example.monew.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class NotificationListenerTest {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private NotificationRepository notificationRepository;

  @Test
  @Transactional
  @DisplayName("댓글 좋아요 이벤트를 발생시키면 비동기로 DB에 알림이 저장된다")
  void testCommentLikedNotification() {
    // given
    UUID myUserId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID likerId = UUID.randomUUID();

    long beforeCount = notificationRepository.count(); // 저장 전 알림 개수
    CommentLikedEvent event = new CommentLikedEvent(myUserId, commentId, likerId);

    // when
    eventPublisher.publishEvent(event);

    // then
    await().atMost(2, SECONDS).untilAsserted(() ->
        assertThat(notificationRepository.count()).isEqualTo(beforeCount + 1)
    );
  }
}