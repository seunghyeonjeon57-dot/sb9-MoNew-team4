package com.example.monew.domain.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

import static com.example.monew.domain.notification.entity.QNotification.notification;

@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public long deleteOldConfirmedNotifications(LocalDateTime threshold) {
    // QueryDSL의 벌크 삭제 연산
    return queryFactory
        .delete(notification)
        .where(
            notification.confirmed.isTrue(),          // 1. 확인한 알림이어야 함
            notification.updatedAt.before(threshold)  // 2. 수정일(확인일)이 기준시간 이전이어야 함
        )
        .execute();
  }
}