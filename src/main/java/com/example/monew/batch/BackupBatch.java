package com.example.monew.batch;

import com.example.monew.batch.service.S3Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupBatch {
  private final ArticleRepository articleRepository;
  private final S3Service s3Service;

  private final ObjectMapper objectMapper; // JSON 변환용

  @Scheduled(cron = "0 0 1 * * *") // 매일 새벽 1시 실행
  public void backupDailyNews() throws JsonProcessingException {
    LocalDate yesterday = LocalDate.now().minusDays(1);
    List<ArticleEntity> articles = articleRepository.findByPublishDateBetween(
        yesterday.atStartOfDay(), yesterday.atTime(LocalTime.MAX));

    if (articles.isEmpty()) return;

    String json = objectMapper.writeValueAsString(articles);
    String fileName = "backup/" + yesterday + "-news.json";

    s3Service.upload(fileName, json);
  }
  public void restoreNews(LocalDate targetDate) {
    log.info("{} 날짜 데이터 S3 복구 시작", targetDate);
    try {
      String fileName = "backup/" + targetDate + "-news.json";
      String json = s3Service.download(fileName);

      if (json == null || json.isEmpty()) {
        throw new RuntimeException("해당 날짜의 백업 파일이 S3에 없습니다.");
      }

      List<ArticleEntity> backupArticles = objectMapper.readValue(json,
          new com.fasterxml.jackson.core.type.TypeReference<List<ArticleEntity>>() {
          });

      int count = 0;
      for (ArticleEntity article : backupArticles) {
        // 핵심: DB에 없는 것만 다시 INSERT (물리 복구)
        if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
          articleRepository.save(article);
          count++;
        }
      }
      log.info("복구 완료: {}건의 기사가 DB에 재등록되었습니다.", count);
    } catch (Exception e) {
      log.error("S3 복구 중 에러 발생", e);
      throw new RuntimeException(e);
    }
  }
}
