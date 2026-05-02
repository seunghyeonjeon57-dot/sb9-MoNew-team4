package com.example.monew.domain.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

  // 이제 Repository가 아니라 Batch 관련 빈(Bean)들을 주입받습니다.
  private final JobLauncher jobLauncher;
  private final Job cleanupNotificationsJob;

  // 매일 새벽 4시에 실행
  @Scheduled(cron = "0 0 4 * * *")
  public void cleanupOldNotifications() {
    try {
      log.info("⏰ [Scheduler] 1주일 경과된 읽은 알림 삭제 배치를 트리거합니다.");

      // Spring Batch는 파라미터가 동일하면 같은 작업으로 인식해서 재실행을 안 합니다.
      // 그래서 '실행 시간'을 파라미터로 넣어 매일 새로운 작업으로 인식하게 만듭니다.
      JobParameters jobParameters = new JobParametersBuilder()
          .addString("requestTime", LocalDateTime.now().toString())
          .toJobParameters();

      // 여기서 실제로 배치 Job이 실행됩니다!
      jobLauncher.run(cleanupNotificationsJob, jobParameters);

      log.info("✅ [Scheduler] 배치 작업이 성공적으로 요청되었습니다.");

    } catch (Exception e) {
      log.error("❌ [Scheduler] 배치 실행 중 에러가 발생했습니다: {}", e.getMessage());
    }
  }
}
