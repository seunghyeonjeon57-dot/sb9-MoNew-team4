package com.example.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.monew.config.QueryDslTestConfig;
import com.example.monew.domain.interest.entity.Subscription;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(QueryDslTestConfig.class)
class SubscriptionRepositoryTest {

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @Test
  @DisplayName("같은 (userId, interestId) 로 2회 저장 시 uk_user_interest 위반 → DataIntegrityViolationException")
  void duplicateSubscription_triggersUniqueConstraintViolation() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    subscriptionRepository.saveAndFlush(
        Subscription.builder().interestId(interestId).userId(userId).build());

    assertThatThrownBy(() -> subscriptionRepository.saveAndFlush(
        Subscription.builder().interestId(interestId).userId(userId).build()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("existsByInterestIdAndUserId: 중복 구독 체크")
  void existsByInterestIdAndUserId() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    subscriptionRepository.save(
        Subscription.builder().interestId(interestId).userId(userId).build());

    assertThat(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId)).isTrue();
    assertThat(subscriptionRepository.existsByInterestIdAndUserId(interestId, UUID.randomUUID())).isFalse();
  }

  @Test
  @DisplayName("findByInterestIdAndUserId: 단건 조회")
  void findByInterestIdAndUserId() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Subscription saved = subscriptionRepository.save(
        Subscription.builder().interestId(interestId).userId(userId).build());

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
    subscriptionRepository.save(
        Subscription.builder().interestId(interestId).userId(UUID.randomUUID()).build());
    subscriptionRepository.save(
        Subscription.builder().interestId(interestId).userId(UUID.randomUUID()).build());

    long deleted = subscriptionRepository.deleteAllByInterestId(interestId);

    assertThat(deleted).isEqualTo(2);
  }

  @Test
  @DisplayName("deleteAllByUserId: 사용자 탈퇴 시 해당 유저의 구독 전체 삭제")
  void deleteAllByUserId() {
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    subscriptionRepository.save(
        Subscription.builder().interestId(UUID.randomUUID()).userId(userId).build());
    subscriptionRepository.save(
        Subscription.builder().interestId(UUID.randomUUID()).userId(userId).build());
    subscriptionRepository.save(
        Subscription.builder().interestId(UUID.randomUUID()).userId(otherUserId).build()); // 다른 유저

    long deleted = subscriptionRepository.deleteAllByUserId(userId);

    assertThat(deleted).isEqualTo(2);
    assertThat(subscriptionRepository.findAllByUserId(userId)).isEmpty();
    assertThat(subscriptionRepository.findAllByUserId(otherUserId)).hasSize(1); // 다른 유저 구독 보존
  }
}
