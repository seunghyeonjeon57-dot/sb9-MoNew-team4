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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ArticleNotificationListener {

  private final InterestRepository interestRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleArticleRegisteredEvent(ArticleRegisteredEvent event) {

    // 1. 이름으로 관심사 엔티티 조회
    interestRepository.findByNameAndDeletedAtIsNull(event.interestName())
        .ifPresent(interest -> {

          // 2. 담당자 가이드에 따라 '벌크 IN 쿼리'로 구독자 조회 (대칭 메서드 사용)
          List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestIdIn(
              List.of(interest.getId())
          );

          // 3. 알림 생성 및 발송
          for (UUID userId : subscriberIds) {
            notificationService.createNotification(new NotificationRequest(
                userId,
                String.format("[%s] 관심 분야의 새 기사가 등록되었습니다: %s",
                    interest.getName(), event.articleTitle()),
                ResourceType.INTEREST,
                event.articleId()
            ));
          }
        });
  }
}