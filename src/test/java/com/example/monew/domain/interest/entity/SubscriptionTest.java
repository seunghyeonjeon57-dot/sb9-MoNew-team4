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

    Subscription sub = new Subscription(interestId, userId);

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
}
