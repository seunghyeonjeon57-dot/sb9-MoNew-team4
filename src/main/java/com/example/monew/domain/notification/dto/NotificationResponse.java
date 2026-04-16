package com.example.monew.domain.notification.dto;

import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    String content,
    ResourceType resourceType,
    UUID resourceId,
    boolean isRead,
    LocalDateTime createdAt
) {
  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getContent(),
        notification.getResourceType(),
        notification.getResourceId(),
        notification.isRead(),
        notification.getCreatedAt()
    );
  }
}