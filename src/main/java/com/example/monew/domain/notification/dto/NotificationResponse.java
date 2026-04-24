package com.example.monew.domain.notification.dto;

import com.example.monew.domain.notification.entity.Notification;
import com.example.monew.domain.notification.entity.ResourceType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    boolean confirmed,
    UUID userId,
    String content,
    ResourceType resourceType,
    UUID resourceId
) {
  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getCreatedAt(),
        notification.getUpdatedAt(),
        notification.isConfirmed(),
        notification.getUserId(),
        notification.getContent(),
        notification.getResourceType(),
        notification.getResourceId()
    );
  }
}