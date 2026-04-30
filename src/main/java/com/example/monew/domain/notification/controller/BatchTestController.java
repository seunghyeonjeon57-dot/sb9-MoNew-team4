package com.example.monew.domain.notification.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
public class BatchTestController {

  private final JobLauncher jobLauncher;
  private final Job cleanupNotificationsJob;

  // 브라우저에서 이 주소로 접속하면 배치가 즉시 실행됩니다!
  @GetMapping("/test/batch/run")
  public String runBatchTest() {
    try {
      log.info("🌐 [Web] 브라우저(8080) 요청으로 배치를 수동 실행합니다.");

      JobParameters jobParameters = new JobParametersBuilder()
          .addString("requestTime", LocalDateTime.now().toString())
          .toJobParameters();

      jobLauncher.run(cleanupNotificationsJob, jobParameters);

      return "<h1>✅ 알림 삭제 배치 실행 완료!</h1><p>인텔리제이 콘솔 로그를 확인해 보세요.</p>";
    } catch (Exception e) {
      log.error("배치 수동 실행 중 에러: {}", e.getMessage());
      return "<h1>❌ 배치 실행 실패</h1><p>에러 로그를 확인하세요.</p>";
    }
  }
}
