package com.example.monew.domain.notification.listener;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.notification.event.ArticleRegisteredEvent;
import com.example.monew.domain.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArticleNotificationListenerTest {

  @Mock
  private InterestRepository interestRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private NotificationService notificationService;

  @InjectMocks
  private ArticleNotificationListener articleNotificationListener;

  @Test
  @DisplayName("기사 등록 이벤트 수신 시, 관심사와 구독자가 모두 존재하면 알림을 생성한다.")
  void handleArticleRegisteredEvent_Success() {
    // Given
    ArticleRegisteredEvent event = new ArticleRegisteredEvent(
        UUID.randomUUID(), "테스트 기사", "테스트 관심사"
    );

    Interest mockInterest = mock(Interest.class);
    given(mockInterest.getName()).willReturn("테스트 관심사");
    given(mockInterest.getId()).willReturn(UUID.randomUUID());

    given(interestRepository.findByNameAndDeletedAtIsNull(anyString()))
        .willReturn(Optional.of(mockInterest));

    given(subscriptionRepository.findUserIdsByInterestIdIn(anyList()))
        .willReturn(List.of(UUID.randomUUID()));

    // When
    articleNotificationListener.handleArticleRegisteredEvent(event);

    // Then
    verify(notificationService, times(1)).createNotifications(anyList());
  }

  @Test
  @DisplayName("관심사는 존재하지만 구독자가 없으면 알림을 생성하지 않는다.")
  void handleArticleRegisteredEvent_NoSubscribers() {
    // Given
    ArticleRegisteredEvent event = new ArticleRegisteredEvent(
        UUID.randomUUID(), "테스트 기사", "테스트 관심사"
    );

    Interest mockInterest = mock(Interest.class);
    given(interestRepository.findByNameAndDeletedAtIsNull(anyString()))
        .willReturn(Optional.of(mockInterest));

    given(mockInterest.getId()).willReturn(UUID.randomUUID());

    given(subscriptionRepository.findUserIdsByInterestIdIn(anyList()))
        .willReturn(List.of());

    // When
    articleNotificationListener.handleArticleRegisteredEvent(event);

    // Then
    verify(notificationService, never()).createNotifications(anyList());
  }

  @Test
  @DisplayName("해당하는 관심사가 존재하지 않으면 아무 작업도 하지 않는다.")
  void handleArticleRegisteredEvent_NoInterest() {
    // Given
    ArticleRegisteredEvent event = new ArticleRegisteredEvent(
        UUID.randomUUID(), "테스트 기사", "존재하지않는관심사"
    );

    given(interestRepository.findByNameAndDeletedAtIsNull(anyString()))
        .willReturn(Optional.empty());

    // When
    articleNotificationListener.handleArticleRegisteredEvent(event);

    // Then
    verify(subscriptionRepository, never()).findUserIdsByInterestIdIn(anyList());
    verify(notificationService, never()).createNotifications(anyList());
  }
}
