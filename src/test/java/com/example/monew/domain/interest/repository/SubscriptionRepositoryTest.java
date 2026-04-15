package com.example.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.interest.entity.Subscription;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class SubscriptionRepositoryTest {

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Test
  @DisplayName("existsByInterestIdAndUserId: 중복 구독 체크")
  void existsByInterestIdAndUserId() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    subscriptionRepository.save(new Subscription(interestId, userId));

    assertThat(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)).isTrue();
    assertThat(subscriptionRepository.existsByInterestIdAndUserId(interestId, UUID.randomUUID())).isFalse();
  }

  @Test
  @DisplayName("findByInterestIdAndUserId: 단건 조회")
  void findByInterestIdAndUserId() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Subscription saved = subscriptionRepository.save(new Subscription(interestId, userId));

    assertThat(subscriptionRepository.findByInterestIdAndUserId(interestId, userId))
        .isPresent()
        .get()
        .extracting(Subscription::getId)
        .isEqualTo(saved.getId());
  }

  @Test
  @DisplayName("deleteAllByInterestId: 인터레스트 삭제 시 구독 정리")
  void deleteAllByInterestId() {
    UUID interestId = UUID.randomUUID();
    subscriptionRepository.save(new Subscription(interestId, UUID.randomUUID()));
    subscriptionRepository.save(new Subscription(interestId, UUID.randomUUID()));

    long deleted = subscriptionRepository.deleteAllByInterestId(interestId);

    assertThat(deleted).isEqualTo(2);
  }
}
