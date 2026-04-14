package com.example.monew.domain.interest.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.monew.domain.interest.dto.SubscriptionDto;
import com.example.monew.domain.interest.entity.InterestSubscription;
import com.example.monew.domain.interest.exception.DuplicateSubscriptionException;
import com.example.monew.domain.interest.exception.InterestNotFoundException;
import com.example.monew.domain.interest.exception.SubscriptionNotFoundException;
import com.example.monew.domain.interest.mapper.InterestMapper;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.InterestSubscriptionRepository;
import java.time.LocalDateTime;
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
  @DisplayName("subscribe: 첫 구독은 저장되고 incrementSubscriberCount가 호출된다")
  void subscribeSucceeds() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(interestRepository.existsById(interestId)).willReturn(true);
    given(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId))
        .willReturn(false);
    InterestSubscription saved = new InterestSubscription(interestId, userId);
    given(subscriptionRepository.save(any(InterestSubscription.class))).willReturn(saved);
    given(interestMapper.toDto(saved))
        .willReturn(new SubscriptionDto(saved.getId(), interestId, userId,
            LocalDateTime.now()));

    service.subscribe(interestId, userId);

    verify(interestRepository).incrementSubscriberCount(interestId);
  }

  @Test
  @DisplayName("subscribe: 중복 구독은 DuplicateSubscriptionException, 카운터 증가 없음")
  void subscribeDuplicate() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(interestRepository.existsById(interestId)).willReturn(true);
    given(subscriptionRepository.existsByInterestIdAndUserId(interestId, userId))
        .willReturn(true);

    assertThatThrownBy(() -> service.subscribe(interestId, userId))
        .isInstanceOf(DuplicateSubscriptionException.class);
    verify(interestRepository, never()).incrementSubscriberCount(any());
  }

  @Test
  @DisplayName("subscribe: 관심사 없음은 InterestNotFoundException")
  void subscribeInterestMissing() {
    UUID missing = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(interestRepository.existsById(missing)).willReturn(false);

    assertThatThrownBy(() -> service.subscribe(missing, userId))
        .isInstanceOf(InterestNotFoundException.class);
  }

  @Test
  @DisplayName("unsubscribe: 구독 중이면 삭제되고 decrementSubscriberCount 호출")
  void unsubscribeSucceeds() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(interestRepository.existsById(interestId)).willReturn(true);
    given(subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId))
        .willReturn(1L);

    service.unsubscribe(interestId, userId);

    verify(interestRepository).decrementSubscriberCount(interestId);
  }

  @Test
  @DisplayName("unsubscribe: 구독하지 않았으면 SubscriptionNotFoundException, 감소 없음")
  void unsubscribeNotSubscribed() {
    UUID interestId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    given(interestRepository.existsById(interestId)).willReturn(true);
    given(subscriptionRepository.deleteByInterestIdAndUserId(interestId, userId))
        .willReturn(0L);

    assertThatThrownBy(() -> service.unsubscribe(interestId, userId))
        .isInstanceOf(SubscriptionNotFoundException.class);
    verify(interestRepository, never()).decrementSubscriberCount(any());
  }

  @Test
  @DisplayName("unsubscribe: 관심사 없음은 InterestNotFoundException")
  void unsubscribeInterestMissing() {
    UUID missing = UUID.randomUUID();
    given(interestRepository.existsById(missing)).willReturn(false);

    assertThatThrownBy(() -> service.unsubscribe(missing, UUID.randomUUID()))
        .isInstanceOf(InterestNotFoundException.class);
  }
}
