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

  @Transactional(readOnly = true)
  public CursorPageResponseNotificationDto getNotifications(UUID userId, String cursor, LocalDateTime after, int limit) {
    Pageable pageable = PageRequest.of(0, limit + 1);
    List<Notification> notifications;

    // String으로 넘어온 커서를 UUID로 변환하여 처리
    UUID cursorUuid = (cursor != null) ? UUID.fromString(cursor) : null;

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

    // 리뷰 반영: 첫 페이지 조회 시에만 전체 개수 카운트 (성능 최적화)
    long totalElements = (cursorUuid == null) ? notificationRepository.countByUserIdAndDeletedAtIsNull(userId) : 0;

    return new CursorPageResponseNotificationDto(content, nextCursor, nextAfter, content.size(), totalElements, hasNext);
  }

  @Transactional
  public void confirmNotification(UUID notificationId, UUID userId) {
    // 수정: 조회 시 userId를 함께 확인하여 타인의 알림 수정을 방지 (보안 검증)
    Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
        .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND));
    notification.confirm();
  }

  @Transactional
  public void confirmAllNotifications(UUID userId) {
    // 수정: 벌크 연산 메서드 호출로 성능 최적화
    notificationRepository.confirmAllByUserId(userId);
  }
}