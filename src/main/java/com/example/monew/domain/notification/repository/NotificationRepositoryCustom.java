package com.example.monew.domain.notification.repository;

import java.time.LocalDateTime;

public interface NotificationRepositoryCustom {

  long deleteOldConfirmedNotifications(LocalDateTime threshold);
}