package com.example.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

  @Test
  @DisplayName("interestId + userId로 구독 생성")
  void createSubscription() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Subscription sub = Subscription.builder()
        .interestId(interestId)
        .userId(userId)
        .build();

    assertThat(sub.getInterestId()).isEqualTo(interestId);
    assertThat(sub.getUserId()).isEqualTo(userId);
  }

  @Test
  @DisplayName("interestId/userId가 null이면 NullPointerException")
  void nullIdsRejected() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> new Subscription(null, id))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Subscription(id, null))
        .isInstanceOf(NullPointerException.class);
  }

  // 아래 두 테스트는 생성자 검증 테스트(nullIdsRejected)와 경로가 동일하지만,
  // @Builder 가 생성자 레벨을 벗어나 클래스 레벨로 이동할 경우 null 가드 우회
  // 경로가 생기는 회귀를 잡기 위한 안전장치로 유지한다.
  @Test
  @DisplayName("빌더 — interestId가 null이면 NullPointerException")
  void builderNullInterestIdRejected() {
    UUID userId = UUID.randomUUID();

    assertThatThrownBy(() -> Subscription.builder().interestId(null).userId(userId).build())
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("빌더 — userId가 null이면 NullPointerException")
  void builderNullUserIdRejected() {
    UUID interestId = UUID.randomUUID();

    assertThatThrownBy(() -> Subscription.builder().interestId(interestId).userId(null).build())
        .isInstanceOf(NullPointerException.class);
  }
}
