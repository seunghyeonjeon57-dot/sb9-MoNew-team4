package com.example.monew.domain.notification.listener;

import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.entity.ResourceType;
import com.example.monew.domain.notification.event.CommentLikedEvent;
import com.example.monew.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CommentNotificationListener {

  private final NotificationService notificationService;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleCommentLikedEvent(CommentLikedEvent event) {

    notificationService.createNotification(new NotificationRequest(
        event.receiverId(),
        // 요구사항: [사용자]님이 나의 댓글을 좋아합니다.
        String.format("[%s]님이 나의 댓글을 좋아합니다.", event.likerName()),
        ResourceType.COMMENT, // 관련 리소스 정보: 댓글
        event.commentId()
    ));
  }
}