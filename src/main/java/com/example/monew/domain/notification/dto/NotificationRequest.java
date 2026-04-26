package com.example.monew.domain.notification.dto;

import com.example.monew.domain.notification.entity.ResourceType;
import java.util.UUID;

public record NotificationRequest(
    UUID userId,
    String content,
    ResourceType resourceType,
    UUID resourceId
) {
}
