package com.example.monew.domain.notification.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

  @Test
  @DisplayName("알림 엔티티 생성 테스트 (UUID 적용)")
  void createNotificationEntity() {
    UUID userId = UUID.randomUUID();
    UUID resourceId = UUID.randomUUID();
    String content = "구독하신 [IT 트렌드] 관련 새 기사가 등록되었습니다.";
    ResourceType resourceType = ResourceType.INTEREST;

    Notification notification = Notification.builder()
        .userId(userId)
        .content(content)
        .resourceType(resourceType)
        .resourceId(resourceId)
        .build();

    assertThat(notification.getUserId()).isEqualTo(userId);
    assertThat(notification.getContent()).isEqualTo(content);
    assertThat(notification.getResourceType()).isEqualTo(resourceType);
    assertThat(notification.getResourceId()).isEqualTo(resourceId);
    assertThat(notification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("알림 읽음 처리 시 isRead 상태가 true로 변경된다.")
  void readNotification() {
    Notification notification = Notification.builder()
        .userId(UUID.randomUUID())
        .content("테스트 알림")
        .resourceType(ResourceType.COMMENT)
        .resourceId(UUID.randomUUID())
        .build();

    notification.confirm();

    assertThat(notification.isConfirmed()).isTrue();
  }
}
