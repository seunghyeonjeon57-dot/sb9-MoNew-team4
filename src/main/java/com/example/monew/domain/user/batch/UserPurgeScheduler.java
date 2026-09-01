package com.example.monew.domain.user.batch;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserPurgeScheduler {

  private final JobLauncher jobLauncher;
  private final Job userPurgeJob;

  // 매일 새벽 4시 10분 실행 (알림 정리 배치가 4시 정각이라 10분 띄움)
  @Scheduled(cron = "0 10 4 * * *")
  public void purgeDeletedUsers() {
    try {
      log.info("⏰ [Scheduler] 1일 경과 탈퇴 유저 물리 삭제 배치를 트리거합니다.");

      // Spring Batch는 파라미터가 동일하면 같은 작업으로 인식해서 재실행을 안 합니다.
      // 그래서 '실행 시간'을 파라미터로 넣어 매일 새로운 작업으로 인식하게 만듭니다.
      JobParameters jobParameters = new JobParametersBuilder()
          .addString("requestDate", LocalDateTime.now().toString())
          .toJobParameters();

      jobLauncher.run(userPurgeJob, jobParameters);

      log.info("✅ [Scheduler] 유저 물리 삭제 배치 작업이 성공적으로 요청되었습니다.");
    } catch (Exception e) {
      log.error("❌ [Scheduler] 유저 물리 삭제 배치 실행 중 에러가 발생했습니다: {}", e.getMessage());
    }
  }
}
