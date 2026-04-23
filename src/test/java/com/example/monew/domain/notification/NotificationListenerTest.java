package com.example.monew.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
class NotificationListenerTest {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private NotificationRepository notificationRepository;

  @MockitoBean
  private S3Client s3Client;

  @MockitoBean
  private com.example.monew.config.NewsBackupBatchConfig newsBackupBatchConfig;

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
    String likerNickname = "테스트유저"; // 닉네임 반영된 구조라면 추가

    CommentLikedEvent event = new CommentLikedEvent(myUserId, commentId, likerId, "테스트유저");

    // when
    eventPublisher.publishEvent(event);

    // then
    await().atMost(2, SECONDS).untilAsserted(() ->
        assertThat(notificationRepository.count()).isGreaterThan(0)
    );
  }
}