package com.example.monew.domain.notification.dto;

import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationResponseTest {

  @Test
  @DisplayName("Notification 엔티티를 NotificationResponse DTO로 올바르게 변환한다.")
  void fromEntity() {
    // Given
    // 1. id를 제외하고 빌더로 객체를 생성합니다.
    Notification notification = Notification.builder()
        .userId(UUID.randomUUID())
        .content("테스트 알림")
        .resourceType(ResourceType.COMMENT)
        .resourceId(UUID.randomUUID())
        .build();

    // 2. ✅ ReflectionTestUtils를 사용해 강제로 id 값을 주입합니다.
    UUID fakeId = UUID.randomUUID();
    ReflectionTestUtils.setField(notification, "id", fakeId);

    // When
    NotificationResponse response = NotificationResponse.from(notification);

    // Then
    assertThat(response.id()).isEqualTo(notification.getId());
    assertThat(response.content()).isEqualTo(notification.getContent());
    assertThat(response.resourceType()).isEqualTo(notification.getResourceType());
    assertThat(response.confirmed()).isEqualTo(notification.isConfirmed());
  }
}