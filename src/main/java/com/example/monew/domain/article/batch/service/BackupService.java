package com.example.monew.domain.article.batch.service;

import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import jakarta.transaction.Transactional;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
  @SneakyThrows
  public void backupDailyNews() {
    LocalDateTime now = LocalDateTime.now().withNano(0);
    String s3Key = "backups/" + now.toString() + ".json";

    JobParameters jobParameters = new JobParametersBuilder()
        .addString("s3Key", s3Key)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(backupJob, jobParameters);
    log.info("데일리 백업 시작 - S3 저장 경로: {}", s3Key);
  }

  public void restoreNewsRange(LocalDateTime from, LocalDateTime to) {
    List<String> errorMessages = new ArrayList<>();

    for (LocalDateTime time = from; !time.isAfter(to); time = time.plusDays(1)) {
      log.info("{} 날짜 데이터 복구 시작", time.toLocalDate());

      try {
        restoreNews(time);
      } catch (Exception e) {
        log.error("{} 날짜 복구 중 실패.", time.toLocalDate());
        errorMessages.add(time.toLocalDate().toString() + " 실패 원인: " + e.getMessage());
      }
    }

    if (!errorMessages.isEmpty()) {
      throw new RestoreFailedException("일부 날짜 복구 실패: " + String.join(", ", errorMessages));
    }
  }

  @SneakyThrows
  private void restoreByFileName(String fileName) {
    File localFile = s3Service.download(fileName);

    JobParameters params = new JobParametersBuilder()
        .addString("filePath", localFile.getAbsolutePath())
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(restoreJob, params);
  }

  @Transactional
  @SneakyThrows
  public void restoreNews(LocalDateTime targetTime) {
    try {
      String fileName = "backups/" + targetTime.withNano(0).toString() + ".json";

      log.info("S3 복구 시도: {}", fileName);
      File localFile = s3Service.download(fileName);

      JobParameters params = new JobParametersBuilder()
          .addString("filePath", localFile.getAbsolutePath())
          .addLong("time", System.currentTimeMillis())
          .toJobParameters();

      jobLauncher.run(restoreJob, params);

    } catch (S3FileNotFoundException e) {
      log.error("파일을 찾을 수 없음: {}", targetTime);
      throw e;
    } catch (Exception e) {
      log.error("복구 실패 - 파일명: {}", targetTime);
      throw new RestoreFailedException(targetTime + " 시점 파일 복구 중 오류", e);
    }
  }
}