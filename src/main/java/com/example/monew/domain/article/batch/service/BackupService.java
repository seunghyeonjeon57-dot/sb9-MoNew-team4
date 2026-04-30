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
    String timestamp = LocalDateTime.now().withSecond(0).withNano(0).toString().replace(":", "-");
    String s3Key = "backups/" + timestamp + ".json";

    JobParameters jobParameters = new
        JobParametersBuilder()
        .addString("s3Key", s3Key)
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();
    log.info(">>>> [Batch] 데일리 백업 실행 시작 - S3 경로: {}", s3Key);
    jobLauncher.run(backupJob, jobParameters);
    log.info("데일리 백업 시작 - S3 저장 경로: {}", s3Key);
  }

  public void restoreNewsRange(LocalDateTime from, LocalDateTime to) {
    List<String> failedDates = new ArrayList<>();
    List<String> successDates = new ArrayList<>();

    for (LocalDateTime time = from; !time.isAfter(to); time = time.plusDays(1)) {
      try {
        restoreNews(time);
        successDates.add(time.toLocalDate().toString());
      } catch (S3FileNotFoundException e) {
        log.warn("{} 날짜는 백업본이 없어 건너뜁니다.", time.toLocalDate());
      } catch (Exception e) {
        log.error("{} 날짜 복구 중 치명적 오류 발생", time.toLocalDate());
        failedDates.add(time.toLocalDate().toString());
      }
    }

    log.info("복구 완료 - 성공: {}, 실패: {}", successDates, failedDates);
  }

  @Transactional
  @SneakyThrows
  public void restoreNews(LocalDateTime targetTime) {
    try {
      String fileName = "backups/" + targetTime
          .withHour(1)    // 새벽 1시 정각 백업본을 찾는다고 가정
          .withMinute(0)
          .withSecond(0)
          .withNano(0)
          .toString()
          .replace(":", "-") + ".json";

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