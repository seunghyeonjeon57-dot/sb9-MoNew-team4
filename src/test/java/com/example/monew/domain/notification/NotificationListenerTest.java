package com.example.monew.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

@Disabled ("수동 확인용 — AFTER_COMMIT 리스너 의존성 이슈로 임시 비활성. 후속 MON-141")
@SpringBootTest
class NotificationListenerTest {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @AfterEach
  void tearDown() {
    notificationRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("댓글 좋아요 이벤트를 발생시키면 비동기로 DB에 알림이 저장된다")
  void testCommentLikedNotification() {
    // given
    UUID myUserId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID likerId = UUID.randomUUID();
    CommentLikedEvent event = new CommentLikedEvent(myUserId, commentId, likerId, "테스트유저");

    // when
    transactionTemplate.executeWithoutResult(status -> {
      eventPublisher.publishEvent(event);
    });

    // then
    await().atMost(2, SECONDS).untilAsserted(() ->
        assertThat(notificationRepository.count()).isGreaterThan(0)
    );
  }
}
