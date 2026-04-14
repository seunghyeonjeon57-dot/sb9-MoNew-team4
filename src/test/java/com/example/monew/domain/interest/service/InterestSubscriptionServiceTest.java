package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestSubscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.time.LocalDateTime;
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
  private InterestSubscriptionRepository subscriptionRepository;

  @Mock
  private InterestMapper interestMapper;

  @InjectMocks
  private InterestSubscriptionService service;

  @Test
  @DisplayName("subscribe: 첫 구독은 저장되고 subscriberCount가 1 증가한다")
  void subscribeSucceeds() {
    UUID userId = UUID.randomUUID();
    Interest interest = new Interest("AI", List.of("k"));
    given(interestRepository.findById(interest.getId())).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByInterestIdAndUserId(interest.getId(), userId))
        .willReturn(false);
    InterestSubscription saved = new InterestSubscription(interest.getId(), userId);
    given(subscriptionRepository.save(any(InterestSubscription.class))).willReturn(saved);
    given(interestMapper.toDto(saved))
        .willReturn(new SubscriptionDto(saved.getId(), interest.getId(), userId,
            LocalDateTime.now()));

    SubscriptionDto result = service.subscribe(interest.getId(), userId);

    assertThat(result.interestId()).isEqualTo(interest.getId());
    assertThat(interest.getSubscriberCount()).isEqualTo(1L);
  }

  @Test
  @DisplayName("subscribe: 중복 구독은 DuplicateSubscriptionException")
  void subscribeDuplicate() {
    UUID userId = UUID.randomUUID();
    Interest interest = new Interest("AI", List.of("k"));
    given(interestRepository.findById(interest.getId())).willReturn(Optional.of(interest));
    given(subscriptionRepository.existsByInterestIdAndUserId(interest.getId(), userId))
        .willReturn(true);

    assertThatThrownBy(() -> service.subscribe(interest.getId(), userId))
        .isInstanceOf(DuplicateSubscriptionException.class);
  }

  @Test
  @DisplayName("subscribe: 관심사 없음은 InterestNotFoundException")
  void subscribeInterestMissing() {
    UUID missing = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(interestRepository.findById(missing)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.subscribe(missing, userId))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("unsubscribe: 구독 중이면 삭제되고 subscriberCount 감소")
  void unsubscribeSucceeds() {
    UUID userId = UUID.randomUUID();
    Interest interest = new Interest("AI", List.of("k"));
    interest.increaseSubscriberCount();
    given(interestRepository.findById(interest.getId())).willReturn(Optional.of(interest));
    given(subscriptionRepository.deleteByInterestIdAndUserId(interest.getId(), userId))
        .willReturn(1L);

    service.unsubscribe(interest.getId(), userId);

    assertThat(interest.getSubscriberCount()).isZero();
    verify(subscriptionRepository).deleteByInterestIdAndUserId(interest.getId(), userId);
  }

  @Test
  @DisplayName("unsubscribe: 구독하지 않았으면 SubscriptionNotFoundException")
  void unsubscribeNotSubscribed() {
    UUID userId = UUID.randomUUID();
    Interest interest = new Interest("AI", List.of("k"));
    given(interestRepository.findById(interest.getId())).willReturn(Optional.of(interest));
    given(subscriptionRepository.deleteByInterestIdAndUserId(interest.getId(), userId))
        .willReturn(0L);

    assertThatThrownBy(() -> service.unsubscribe(interest.getId(), userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
  }

  @Test
  @DisplayName("unsubscribe: 관심사 없음은 InterestNotFoundException")
  void unsubscribeInterestMissing() {
    UUID missing = UUID.randomUUID();
    given(interestRepository.findById(missing)).willReturn(Optional.empty());

    assertThatThrownBy(() -> service.unsubscribe(missing, UUID.randomUUID()))
        .isInstanceOf(InterestNotFoundException.class);
  }
}
