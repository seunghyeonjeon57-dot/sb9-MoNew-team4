package com.example.monew.domain.article.batch.service;

import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import jakarta.transaction.Transactional;
import java.io.File;
import java.time.LocalDateTime;
import lombok.SneakyThrows;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobParameters;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupService {
  private final S3Service s3Service;
  private final Job restoreJob;
  private final JobLauncher jobLauncher;
  private final Job backupJob;

  @Scheduled(cron = "0 0 1 * * *")
  @SneakyThrows //롬북 - 모든 예외를 자동으로
  public void backupDailyNews() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(backupJob, jobParameters);

  }
  public void restoreNewsRange(LocalDateTime from, LocalDateTime to) {
    LocalDate startDate = from.toLocalDate();
    LocalDate endDate = to.toLocalDate();

    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      try {
        log.info("{} 날짜 데이터 복구 시작", date);
        restoreNews(date);
      } catch (Exception e) {
        log.error("{} 날짜 복구 중 실패. 다음 날짜로 넘어갑니다.", date, e);
      }
    }
  }
  @Transactional
  public void restoreNews(LocalDate targetDate) {
    try {
      String fileName = "backups/" + targetDate + ".json";
      File localFile = s3Service.download(fileName);

      JobParameters params = new JobParametersBuilder()
          .addString("filePath", localFile.getAbsolutePath())
          .addLong("time", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(restoreJob, params);

    } catch (Exception e) {
      log.error("복구 배치 실행 실패", e);
      throw new RestoreFailedException(targetDate + " 날짜 기사 복구 중 오류 발생", e);
    }
  }

}
