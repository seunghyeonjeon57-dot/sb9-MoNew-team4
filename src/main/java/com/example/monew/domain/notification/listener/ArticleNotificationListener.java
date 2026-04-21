package com.example.monew.domain.notification.listener;

import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.event.ArticleRegisteredEvent;
import com.example.monew.domain.notification.service.NotificationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArticleNotificationListener {

  private final SubscriptionRepository subscriptionRepository;
  private final NotificationService notificationService;
  private final InterestRepository interestRepository; // 의존성 주입

  @Async
  @EventListener
  public void handleArticleRegisteredEvent(ArticleRegisteredEvent event) {

    // 1. 코드를 확인해서 찾은 정확한 메서드 사용
    Interest interest = interestRepository.findByNameAndDeletedAtIsNull(event.interestName())
        .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 삭제된 관심사입니다: " + event.interestName()));

    // 2. 관심사 ID를 이용해 구독자 ID 목록 조회
    List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestIdIn(List.of(interest.getId()));

    if (subscriberIds.isEmpty()) {
      return;
    }

    // 3. 다건 알림 리스트 생성
    List<NotificationRequest> requests = subscriberIds.stream()
        .map(userId -> new NotificationRequest(
            userId,
            String.format("[%s]와 관련된 기사가 등록되었습니다.", interest.getName()),
            ResourceType.INTEREST,
            event.articleId()
        ))
        .toList();

    // 4. 단 한 번의 호출로 N건 저장
    notificationService.createNotifications(requests);
  }
}