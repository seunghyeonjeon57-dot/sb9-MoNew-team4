package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.monew.domain.interest.dto.SubscriptionResponse;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.Subscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

  @InjectMocks
  private InterestSubscriptionService service;

  @Test
  @DisplayName("subscribe: 정상 → saveAndFlush + incrementSubscriberCount 호출")
  void subscribeSuccess() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionResponse response = service.subscribe(interest.getId(), userId);

    assertThat(response.userId()).isEqualTo(userId);
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
  @DisplayName("subscribe: DB unique 위반 → DuplicateSubscriptionException 변환")
  void subscribeDuplicate() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndDeletedAtIsNull(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.saveAndFlush(any(Subscription.class)))
        .thenThrow(new DataIntegrityViolationException("uk_user_interest"));

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(DuplicateSubscriptionException.class);
  }

  @Test
  @DisplayName("unsubscribe: 정상 → 삭제 + decrementSubscriberCount 호출")
  void unsubscribeSuccess() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Subscription sub = new Subscription(interestId, userId);
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
