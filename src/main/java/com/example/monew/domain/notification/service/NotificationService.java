package com.example.monew.domain.notification.service;

import com.example.monew.domain.notification.dto.CursorPageResponseNotificationDto;
import com.example.monew.domain.notification.dto.NotificationRequest;
import com.example.monew.domain.notification.dto.NotificationResponse;
import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.exception.NotificationException;
import com.example.monew.domain.notification.repository.NotificationRepository;
import com.example.monew.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

  @Transactional
  public void createNotifications(List<NotificationRequest> requests) {
    List<Notification> notifications = requests.stream()
        .map(req -> Notification.builder()
            .userId(req.userId())
            .content(req.content())
            .resourceType(req.resourceType())
            .resourceId(req.resourceId())
            .build())
        .toList();

    notificationRepository.saveAll(notifications);
  }

  @Transactional(readOnly = true)
  public CursorPageResponseNotificationDto getNotifications(UUID userId, String cursor, LocalDateTime after, int limit) {
    Pageable pageable = PageRequest.of(0, limit + 1);
    List<Notification> notifications;

    UUID cursorUuid = null;
    if (cursor != null && !cursor.isBlank()) {
      try {
        cursorUuid = UUID.fromString(cursor);
      } catch (IllegalArgumentException e) {
        throw new NotificationException(ErrorCode.INVALID_REQUEST);
      }
    }

    if (cursorUuid == null || after == null) {
      notifications = notificationRepository.findFirstPageByUserId(userId, pageable);
    } else {
      notifications = notificationRepository.findNextPageByUserId(userId, after, cursorUuid, pageable);
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

    long totalElements = (cursorUuid == null) ? notificationRepository.countByUserIdAndConfirmedFalseAndDeletedAtIsNull(userId) : 0;

    return new CursorPageResponseNotificationDto(content, nextCursor, nextAfter, content.size(), totalElements, hasNext);
  }

  @Transactional
  public void confirmNotification(UUID notificationId, UUID userId) {
    Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
        .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));
    notification.confirm();
  }

  @Transactional
  public int confirmAllNotifications(UUID userId) {
    return notificationRepository.confirmAllByUserId(userId);
  }
}
