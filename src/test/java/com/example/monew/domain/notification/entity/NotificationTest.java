package com.example.monew.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  @DisplayName("알림 객체가 정상적으로 생성되며, 초기 읽음 상태는 false이다.")
  void createNotification() {
    // given (준비)
    Long userId = 1L;
    String content = "구독하신 [IT 트렌드] 관련 새 기사가 등록되었습니다.";
    ResourceType resourceType = ResourceType.INTEREST;
    Long resourceId = 10L;

    // when (실행)
    Notification notification = Notification.builder()
        .userId(userId)
        .content(content)
        .resourceType(resourceType)
        .resourceId(resourceId)
        .build();

    // then (검증)
    assertThat(notification.getUserId()).isEqualTo(userId);
    assertThat(notification.getContent()).isEqualTo(content);
    assertThat(notification.getResourceType()).isEqualTo(resourceType);
    assertThat(notification.getResourceId()).isEqualTo(resourceId);
    assertThat(notification.isRead()).isFalse();
  }

  @Test
  @DisplayName("알림 읽음 처리(read) 메서드 호출 시 isRead 상태가 true로 변경된다.")
  void readNotification() {
    // given (준비)
    Notification notification = Notification.builder()
        .userId(1L)
        .content("우디님이 내 댓글을 좋아합니다.")
        .resourceType(ResourceType.COMMENT)
        .resourceId(20L)
        .build();

    assertThat(notification.isRead()).isFalse();

    // when (실행)
    notification.read();

    // then (검증)
    assertThat(notification.isRead()).isTrue();
  }
}