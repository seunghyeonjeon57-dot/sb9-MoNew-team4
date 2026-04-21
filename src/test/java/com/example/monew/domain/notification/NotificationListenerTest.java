package com.example.monew.domain.notification;

import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.notification.repository.NotificationRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;

@SpringBootTest
class NotificationListenerTest {

  @Autowired
  private ApplicationEventPublisher eventPublisher;

  @Autowired
  private NotificationRepository notificationRepository; // 확인용

  @Test
  @DisplayName("댓글 좋아요 이벤트를 강제로 발생시키면 DB에 알림이 저장된다")
  void testCommentLikedNotification() {
    // given: 가짜 데이터로 이벤트 생성
    UUID myUserId = UUID.randomUUID(); // 알림 받을 사람 ID
    UUID commentId = UUID.randomUUID();
    String likerName = "테스트유저";

    CommentLikedEvent event = new CommentLikedEvent(myUserId, commentId, likerName);

    // when: 내가 직접 우체부(Publisher)가 되어서 이벤트를 쏴본다!
    eventPublisher.publishEvent(event);

    // then: DB(notifications 테이블)에 알림이 진짜 저장되었는지 눈으로 확인 (또는 코드 단언)
    // DBeaver 같은 DB 툴을 열어서 방금 쏜 알림이 들어왔는지 확인하시면 끝납니다!
  }
}
