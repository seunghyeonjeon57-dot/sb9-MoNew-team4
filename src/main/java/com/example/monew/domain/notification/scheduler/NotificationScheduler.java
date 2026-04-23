package com.example.monew.domain.notification.scheduler;

import com.example.monew.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  private final NotificationRepository notificationRepository;


// 매일 새벽 4시에 실행 (1주일 지난 확인된 알림 삭제)
  @Scheduled(cron = "0 0 4 * * *")
  @Transactional
  public void cleanupOldNotifications() {
    log.info("⏰ [Batch] 1주일 경과된 읽은 알림 삭제 배치를 시작합니다.");

    // 기준 시간: 현재 시간으로부터 딱 1주일 전
    LocalDateTime threshold = LocalDateTime.now().minusWeeks(1);

    // 우리가 방금 만든 QueryDSL 메서드 호출
    long deletedCount = notificationRepository.deleteOldConfirmedNotifications(threshold);

    log.info("✅ [Batch] 총 {}개의 오래된 알림이 DB에서 물리적으로 삭제되었습니다.", deletedCount);
  }
}