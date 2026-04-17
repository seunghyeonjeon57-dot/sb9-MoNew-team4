package com.example.monew.domain.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CursorPageResponseNotificationDto(
    List<NotificationResponse> content,
    String nextCursor,
    LocalDateTime nextAfter,
    int size,
    long totalElements,
    boolean hasNext
) {}