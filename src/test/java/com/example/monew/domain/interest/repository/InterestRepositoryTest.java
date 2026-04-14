package com.example.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.monew.config.JpaAuditConfig;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestSubscription;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(JpaAuditConfig.class)
@ActiveProfiles("test")
class InterestRepositoryTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private InterestSubscriptionRepository subscriptionRepository;

  @Test
  @DisplayName("Interest 저장 후 ID로 조회하면 엔티티가 반환된다")
  void saveAndFindInterest() {
    Interest interest = new Interest("인공지능", List.of("AI", "ML"));

    Interest saved = interestRepository.saveAndFlush(interest);

    Interest found = interestRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("인공지능");
    assertThat(found.getKeywords()).containsExactlyInAnyOrder("AI", "ML");
    assertThat(found.getSubscriberCount()).isZero();
    assertThat(found.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("동일한 이름으로 두 개의 Interest를 저장하면 unique 제약 위반 예외가 발생한다")
  void duplicateNameViolatesUniqueConstraint() {
    interestRepository.saveAndFlush(new Interest("인공지능", List.of("AI")));

    assertThatThrownBy(() ->
        interestRepository.saveAndFlush(new Interest("인공지능", List.of("ML")))
    ).isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("isDeleted=false 필터로 soft-deleted 제외 조회가 가능하다")
  void findAllByIsDeletedFalse() {
    Interest live = interestRepository.saveAndFlush(new Interest("라이브", List.of("k")));
    interestRepository.saveAndFlush(new Interest("삭제됨", List.of("k")));

    List<Interest> all = interestRepository.findAllByIsDeletedFalse();

    assertThat(all).extracting(Interest::getId).contains(live.getId());
  }

  @Test
  @DisplayName("InterestSubscription 동일 (interestId, userId)는 unique 제약 위반이다")
  void duplicateSubscriptionViolatesUnique() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    subscriptionRepository.saveAndFlush(new InterestSubscription(interestId, userId));

    assertThatThrownBy(() ->
        subscriptionRepository.saveAndFlush(new InterestSubscription(interestId, userId))
    ).isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("findInterestIdsByUserIdAndInterestIdIn은 해당 유저가 구독 중인 관심사 ID만 반환한다")
  void findSubscribedInterestIds() {
    UUID userId = UUID.randomUUID();
    UUID i1 = UUID.randomUUID();
    UUID i2 = UUID.randomUUID();
    UUID i3 = UUID.randomUUID();
    subscriptionRepository.saveAndFlush(new InterestSubscription(i1, userId));
    subscriptionRepository.saveAndFlush(new InterestSubscription(i2, userId));
    subscriptionRepository.saveAndFlush(new InterestSubscription(i3, UUID.randomUUID()));

    List<UUID> subscribed = subscriptionRepository.findInterestIdsByUserIdAndInterestIdIn(
        userId, List.of(i1, i2, i3));

    assertThat(subscribed).containsExactlyInAnyOrder(i1, i2);
  }
}
