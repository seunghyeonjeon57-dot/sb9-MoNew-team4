package com.example.monew.domain.notification.listener;

import com.example.monew.domain.interest.repository.InterestRepository;
import com.example.monew.domain.interest.repository.SubscriptionRepository;
import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.event.ArticleRegisteredEvent;
import com.example.monew.domain.notification.service.NotificationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArticleNotificationListener {

  private final SubscriptionRepository subscriptionRepository;
  private final NotificationService notificationService;
  private final InterestRepository interestRepository;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleArticleRegisteredEvent(ArticleRegisteredEvent event) {

    interestRepository.findByNameAndDeletedAtIsNull(event.interestName())
        .ifPresentOrElse(
            interest -> {
              List<UUID> subscriberIds = subscriptionRepository.findUserIdsByInterestIdIn(List.of(interest.getId()));

              if (subscriberIds.isEmpty()) return;

              List<NotificationRequest> requests = subscriberIds.stream()
                  .map(userId -> new NotificationRequest(
                      userId,
                      String.format("[%s]와 관련된 기사가 등록되었습니다.", interest.getName()),
                      ResourceType.INTEREST,
                      event.articleId()
                  ))
                  .toList();

              notificationService.createNotifications(requests);
            },
            () -> log.debug("알림 스킵 — 매칭되는 관심사 없음: {}", event.interestName())
        );
  }
}