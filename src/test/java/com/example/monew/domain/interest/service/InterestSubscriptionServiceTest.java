package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

@ExtendWith(MockitoExtension.class)
class InterestSubscriptionServiceTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @InjectMocks
  private InterestSubscriptionService service;

  @Test
  @DisplayName("subscribe: 정상 → 저장 + subscriberCount 증가")
  void subscribeSuccess() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndIsDeletedFalse(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.existsByInterestIdAndUserId(interest.getId(), userId)).thenReturn(false);
    when(subscriptionRepository.save(any(Subscription.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionResponse response = service.subscribe(interest.getId(), userId);

    assertThat(response.userId()).isEqualTo(userId);
    assertThat(interest.getSubscriberCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("subscribe: 미존재 인터레스트 → InterestNotFoundException")
  void subscribeInterestNotFound() {
    UUID id = UUID.randomUUID();
    when(interestRepository.findByIdAndIsDeletedFalse(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.subscribe(id, UUID.randomUUID()))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("subscribe: 이미 구독 → DuplicateSubscriptionException")
  void subscribeDuplicate() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    UUID userId = UUID.randomUUID();
    when(interestRepository.findByIdAndIsDeletedFalse(interest.getId()))
        .thenReturn(Optional.of(interest));
    when(subscriptionRepository.existsByInterestIdAndUserId(interest.getId(), userId)).thenReturn(true);

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(DuplicateSubscriptionException.class);
  }

  @Test
  @DisplayName("unsubscribe: 정상 → 삭제 + subscriberCount 감소")
  void unsubscribeSuccess() {
    Interest interest = new Interest("인공지능", List.of("AI"));
    interest.incrementSubscriberCount();
    UUID userId = UUID.randomUUID();
    Subscription sub = new Subscription(interest.getId(), userId);
    when(subscriptionRepository.findByInterestIdAndUserId(interest.getId(), userId))
        .thenReturn(Optional.of(sub));
    when(interestRepository.findByIdAndIsDeletedFalse(interest.getId()))
        .thenReturn(Optional.of(interest));

    service.unsubscribe(interest.getId(), userId);

    verify(subscriptionRepository).delete(sub);
    assertThat(interest.getSubscriberCount()).isZero();
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
