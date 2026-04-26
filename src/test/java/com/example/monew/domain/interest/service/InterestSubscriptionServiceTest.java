package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.monew.domain.activity.service.ActivityService;
import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.Subscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriberNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InterestSubscriptionServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private ActivityService activityService;

  @InjectMocks
  private InterestSubscriptionService service;

  @Test
  @DisplayName("subscribe: 정상 → saveAndFlush + incrementSubscriberCount + SubscriptionDto 반환")
  void subscribeSuccess() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI", "ML")).build();
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionResponse response = service.subscribe(interest.getId(), userId);

    assertThat(response.interestId()).isEqualTo(interest.getId());
    assertThat(response.interestName()).isEqualTo("인공지능");
    assertThat(response.interestKeywords()).containsExactly("AI", "ML");
    assertThat(response.interestSubscriberCount()).isEqualTo(0L);
    verify(interestRepository).incrementSubscriberCount(eq(interest.getId()));
  }

  @Test
  @DisplayName("subscribe: 미존재 인터레스트 → InterestNotFoundException")
  void subscribeInterestNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.subscribe(id, UUID.randomUUID()))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("subscribe: UNIQUE(uk_user_interest) 위반 → DuplicateSubscriptionException(409)")
  void subscribeDuplicate() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .thenThrow(dataIntegrityViolation("uk_user_interest"));

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(DuplicateSubscriptionException.class);
  }

  @Test
  @DisplayName("subscribe: FK(fk_sub_user) 위반 → SubscriberNotFoundException(404)")
  void subscribeFkUserViolation() {
    Interest interest = Interest.builder().name("경제").keywords(List.of("금리")).build();
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .thenThrow(dataIntegrityViolation("fk_sub_user"));

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(SubscriberNotFoundException.class);
  }

  @Test
  @DisplayName("subscribe: FK(fk_sub_interest) 위반(race) → InterestNotFoundException(404)")
  void subscribeFkInterestViolation() {
    Interest interest = Interest.builder().name("스포츠").keywords(List.of("축구")).build();
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .thenThrow(dataIntegrityViolation("fk_sub_interest"));

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(InterestNotFoundException.class);
  }

  private static DataIntegrityViolationException dataIntegrityViolation(String constraintName) {
    ConstraintViolationException cve = new ConstraintViolationException(
        constraintName, new SQLException(), constraintName);
    return new DataIntegrityViolationException("constraint violation", cve);
  }

  // [MON-146] 스웨거(docs/monew-swagger.json) POST /api/interests/{id}/subscriptions 의
  // 명시 응답 코드는 200 / 404 / 500 뿐이며 409 DUPLICATE_SUBSCRIPTION 은 누락되어 있음.
  // 현 구현은 원인 불명 DataIntegrityViolationException 을 DuplicateSubscriptionException(409)
  // 으로 폴백시키는 보수적 정책을 사용한다. 본 테스트는 해당 폴백 동작을 고정하는 목적이며,
  // 스웨거에 409 응답이 추가되거나 폴백 정책이 변경되면 함께 갱신해야 한다.
  @Test
  @DisplayName("[MON-146] subscribe: DIVE 원인이 ConstraintViolationException이 아니면 DuplicateSubscriptionException 으로 폴백")
  void subscribeDataIntegrityWithNonConstraintViolationCause() {
    Interest interest = Interest.builder().name("인공지능").keywords(List.of("AI")).build();
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    DataIntegrityViolationException unknown = new DataIntegrityViolationException(
        "unknown cause", new IllegalStateException("non-constraint"));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class))).thenThrow(unknown);

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(DuplicateSubscriptionException.class);
  }

  @Test
  @DisplayName("unsubscribe: 정상 → 삭제 + decrementSubscriberCount 호출")
  void unsubscribeSuccess() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Subscription sub = Subscription.builder().interestId(interestId).userId(userId).build();
    when(subscriptionRepository.findByInterestIdAndUserId(interestId, userId))
        .thenReturn(Optional.of(sub));

    service.unsubscribe(interestId, userId);

    verify(subscriptionRepository).delete(sub);
    verify(interestRepository).decrementSubscriberCount(eq(interestId));
  }

  @Test
  @DisplayName("unsubscribe: 미구독 → SubscriptionNotFoundException")
  void unsubscribeNotFound() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(subscriptionRepository.findByInterestIdAndUserId(interestId, userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.unsubscribe(interestId, userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

}
