package com.example.monew.domain.notification.service;

import com.example.monew.domain.notification.dto.CursorPageResponseNotificationDto;
import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.dto.NotificationResponse;
import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.exception.NotificationException;
import com.example.monew.domain.notification.repository.NotificationRepository;
import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;

  @Transactional
  public UUID createNotification(NotificationRequest request) {
    Notification notification = Notification.builder()
        .userId(request.userId())
        .content(request.content())
        .resourceType(request.resourceType())
        .resourceId(request.resourceId())
        .build();

    return notificationRepository.save(notification).getId();
  }

  @Transactional(readOnly = true)
  public CursorPageResponseNotificationDto getNotifications(UUID userId, String cursor, LocalDateTime after, int limit) {
    Pageable pageable = PageRequest.of(0, limit + 1);
    List<Notification> notifications;

    if (cursor == null || after == null) {
      notifications = notificationRepository.findFirstPageByUserId(userId, pageable);
    } else {
      notifications = notificationRepository.findNextPageByUserId(userId, after, cursor, pageable);
    }

    boolean hasNext = notifications.size() > limit;
    if (hasNext) {
      notifications = notifications.subList(0, limit);
    }

    List<NotificationResponse> content = notifications.stream()
        .map(NotificationResponse::from)
        .toList();

    String nextCursor = null;
    LocalDateTime nextAfter = null;
    if (hasNext && !content.isEmpty()) {
      NotificationResponse lastItem = content.get(content.size() - 1);
      nextCursor = lastItem.id().toString();
      nextAfter = lastItem.createdAt();
    }

    long totalElements = notificationRepository.countByUserIdAndDeletedAtIsNull(userId);

    return new CursorPageResponseNotificationDto(
        content,
        nextCursor,
        nextAfter,
        content.size(),
        totalElements,
        hasNext
    );
  }

  @Transactional
  public void readNotification(UUID notificationId) {
    Notification notification = notificationRepository.findByIdAndDeletedAtIsNull(notificationId)
        .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));

    notification.confirm();
  }

  @Transactional
  public void confirmAllNotifications(UUID userId) {
    // 삭제되지 않고 아직 확인되지 않은 모든 알림을 찾아서 확인 처리
    List<Notification> notifications = notificationRepository.findAllByUserIdAndConfirmedFalseAndDeletedAtIsNull(userId);
    notifications.forEach(Notification::confirm);
  }
}