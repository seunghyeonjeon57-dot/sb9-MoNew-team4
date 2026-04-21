package com.example.monew.domain.article.batch.service;


import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.io.File;
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
  private final ArticleRepository articleRepository;
  private final S3Service s3Service;
  private final Job restoreJob;
  private final JobLauncher jobLauncher;
  private final Job backupJob;

  private final ObjectMapper objectMapper; // JSON 변환용

  @Scheduled(cron = "0 0 1 * * *")
  @SneakyThrows //롬북 - 모든 예외를 자동으로
  public void backupDailyNews() {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("time", System.currentTimeMillis())
        .toJobParameters();

    jobLauncher.run(backupJob, jobParameters);

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
    }
  }

}
