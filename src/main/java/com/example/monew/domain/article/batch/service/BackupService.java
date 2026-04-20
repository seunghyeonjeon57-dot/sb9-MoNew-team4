package com.example.monew.domain.article.batch.service;


import com.example.monew.config.NewsBackupBatchConfig;
import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.io.File;
import lombok.SneakyThrows;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
//    log.info("{} 날짜 데이터 S3 복구 시작", targetDate);
//    try {
//      String fileName = "backups/" + targetDate + "-news.json";
//      String json = s3Service.download(fileName);
//
//      if (json == null || json.isEmpty()) {
//        throw new RuntimeException("해당 날짜의 백업 파일이 S3에 없습니다.");
//      }
//
//      List<ArticleEntity> backupArticles = objectMapper.readValue(
//          json, new TypeReference<List<ArticleEntity>>() {}
//      );
//
//      if (backupArticles.isEmpty()) {
//        log.info("복구할 데이터 없음");
//        return;
//      }
//
//      List<String> sourceUrls = backupArticles.stream()
//          .map(ArticleEntity::getSourceUrl)
//          .toList();
//
//      List<ArticleEntity> existingArticles =
//          articleRepository.findAllBySourceUrlIn(sourceUrls);
//
//      Set<String> existingUrls = existingArticles.stream()
//          .map(ArticleEntity::getSourceUrl)
//          .collect(Collectors.toSet());
//
//      List<ArticleEntity> toSave = backupArticles.stream()
//          .filter(article -> !existingUrls.contains(article.getSourceUrl()))
//          .toList();
//
//      articleRepository.saveAll(toSave);
//
//      log.info("복구 완료: {}건 저장됨", toSave.size());
//
//    }  catch (S3FileNotFoundException e) {
//      log.warn("백업 파일 없음: {}", targetDate);
//      throw e;
//
//    } catch (Exception e) {
//      log.error("복구 중 에러 발생 - targetDate: {}", targetDate, e);
//      throw new RestoreFailedException("뉴스 복구 실패", e);
//    }
//  }
}
