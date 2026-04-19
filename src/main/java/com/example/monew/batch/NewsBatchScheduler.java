package com.example.monew.batch;

import com.example.monew.batch.service.S3Service;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBatchScheduler {
  private final NewsRss newsRss;
  private final NewsCollector newsCollector;
  private final ArticleRepository articleRepository;

  private final S3Service s3Service;

  private final ObjectMapper objectMapper; // JSON 변환



  @Scheduled(cron = "0 0 * * * *")
  public void runNewsBatch() {
    log.info("뉴스 수집 배치 시작");

    restoreFromS3();

    List<String> rssUrls = newsRss.getUrlList();
    for (String url : rssUrls) {
      try {
        List<ArticleEntity> candidates = newsCollector.fetchRss(url, "RSS수집원", "뉴스");

        saveInChunks(candidates);

      } catch (Exception e) {
        log.error("URL 수집 중 오류 발생 (해당 사이트만 스킵): {}", url, e);
      }
    }
    backupToS3();
    log.info("뉴스 배치 프로세스 전체 종료");
  }

  @Transactional
  public void saveInChunks(List<ArticleEntity> articles) {
    if (articles.isEmpty()) return;
    List<String> sourceUrls = articles.stream()
        .map(ArticleEntity::getSourceUrl)
        .toList();

    List<String> existingUrls = articleRepository.findAllBySourceUrlIn(sourceUrls)
        .stream()
        .map(ArticleEntity::getSourceUrl)
        .toList();

    List<ArticleEntity> newArticles = articles.stream()
        .filter(article -> !existingUrls.contains(article.getSourceUrl()))
        .toList();

    if (!newArticles.isEmpty()) {
      articleRepository.saveAll(newArticles);
      log.info("{}건 신규 뉴스 저장 완료", newArticles.size());
    }
  }

  private void backupToS3() {
    try {
      List<ArticleEntity> todayArticles = articleRepository.findByPublishDateAfter(
          java.time.LocalDate.now().atStartOfDay()
      );

      String json = objectMapper.writeValueAsString(todayArticles);
      String fileName = "backups/" + java.time.LocalDate.now() + ".json";
      s3Service.upload(fileName, json);

      log.info("S3 백업 완료: {}", fileName);

    } catch (Exception e) {
      log.error("S3 백업 중 오류 발생", e);
    }
  }

  private void restoreFromS3() {
    try {
      String fileName = "backups/" + java.time.LocalDate.now() + ".json";
      String json = s3Service.download(fileName);

      if (json == null || json.isEmpty()) return;

      List<ArticleEntity> backupArticles = objectMapper.readValue(json,
          new TypeReference<List<ArticleEntity>>() {});

      int restoreCount = 0;
      for (ArticleEntity article : backupArticles) {
        if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
          articleRepository.save(article);
          restoreCount++;
        }
      }
      if (restoreCount > 0) log.warn("S3로부터 {}건의 데이터 복구 완료", restoreCount);
    } catch (Exception e) {
      log.info("오늘자 백업 파일이 없거나 복구할 데이터가 없습니다.");
    }
  }

}