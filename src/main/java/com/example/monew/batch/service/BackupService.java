package com.example.monew.batch.service;


import com.example.monew.batch.exception.RestoreFailedException;
import com.example.monew.batch.exception.S3FileNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupService {
  private final ArticleRepository articleRepository;
  private final S3Service s3Service;

  private final ObjectMapper objectMapper; // JSON 변환용

  @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시 실행
  public void backupDailyNews() {
    try {
      LocalDate yesterday = LocalDate.now().minusDays(1);

      List<ArticleEntity> articles =
          articleRepository.findByPublishDateBetween(
              yesterday.atStartOfDay(),
              yesterday.atTime(LocalTime.MAX)
          );

      if (articles.isEmpty()) return;

      String json = objectMapper.writeValueAsString(articles);
      String fileName = "backup/" + yesterday + "-news.json";

      s3Service.upload(fileName, json);

    } catch (JsonProcessingException e) {
      log.error("백업 JSON 변환 실패", e);// BackupFailedException 예정
    }
  }

  @Transactional
  public void restoreNews(LocalDate targetDate) {
    log.info("{} 날짜 데이터 S3 복구 시작", targetDate);
    try {
      String fileName = "backup/" + targetDate + "-news.json";
      String json = s3Service.download(fileName);

      if (json == null || json.isEmpty()) {
        throw new RuntimeException("해당 날짜의 백업 파일이 S3에 없습니다.");
      }

      List<ArticleEntity> backupArticles = objectMapper.readValue(
          json, new TypeReference<List<ArticleEntity>>() {}
      );

      if (backupArticles.isEmpty()) {
        log.info("복구할 데이터 없음");
        return;
      }

      List<String> sourceUrls = backupArticles.stream()
          .map(ArticleEntity::getSourceUrl)
          .toList();

      List<ArticleEntity> existingArticles =
          articleRepository.findAllBySourceUrlIn(sourceUrls);

      Set<String> existingUrls = existingArticles.stream()
          .map(ArticleEntity::getSourceUrl)
          .collect(Collectors.toSet());

      List<ArticleEntity> toSave = backupArticles.stream()
          .filter(article -> !existingUrls.contains(article.getSourceUrl()))
          .toList();

      articleRepository.saveAll(toSave);

      log.info("복구 완료: {}건 저장됨", toSave.size());

    }  catch (S3FileNotFoundException e) {
      log.warn("백업 파일 없음: {}", targetDate);
      throw e;

    } catch (Exception e) {
      log.error("복구 중 에러 발생 - targetDate: {}", targetDate, e);
      throw new RestoreFailedException("뉴스 복구 실패", e);
    }
  }
}
