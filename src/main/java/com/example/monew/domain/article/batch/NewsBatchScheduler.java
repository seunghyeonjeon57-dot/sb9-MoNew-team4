package com.example.monew.domain.article.batch;

import com.example.monew.domain.article.batch.service.BackupService;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.interest.entity.InterestKeyword;
import com.example.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBatchScheduler {

  private final NewsCollector newsCollector;
  private final NewsRss newsRss;
  private final ArticleRepository articleRepository;
  private final BackupService backupService;
  private final InterestRepository interestRepository;
  private final ArticleService articleService;

  @Scheduled(cron = "0 0/15 * * * *")
  @Transactional(readOnly = false)
  public void runNewsBatch() {
    log.info("=== 뉴스 배치 수집 시작 ===");

    List<String> keywords = interestRepository.findAll().stream()
        .flatMap(interest -> interest.getKeywords().stream())
        .map(InterestKeyword::getValue)
        .distinct()
        .filter(kw -> kw != null && !kw.isBlank()) // 빈 값 방어
        .toList();

    if (keywords.isEmpty()) {
      log.warn("수집할 키워드가 없습니다.");
      return;
    }

    for (String keyword : keywords) {
      List<ArticleEntity> naverArticles = newsCollector.fetchNaver(keyword);

      articleService.saveIfUnique(naverArticles);

      newsRss.getRss().forEach((pressName, url) -> {
        List<ArticleEntity> rssArticles = newsCollector.fetchRss(url, pressName, keyword);
        articleService.saveIfUnique(rssArticles);
      });
    }

    log.info("=== 뉴스 수집 및 저장 완료 ===");
  }

  private void saveUniqueArticles(List<ArticleEntity> articles) {
    if (articles == null || articles.isEmpty()) {
      log.warn(">>> [확인] 수집된 기사가 0건입니다. 키워드나 API 설정을 확인하세요.");
      return;
    }

    for (ArticleEntity article : articles) {
      boolean isExist = articleRepository.existsBySourceUrl(article.getSourceUrl());

      if (!isExist) {
        articleRepository.save(article);
        log.info(">>> [DB 저장 성공] 제목: {}", article.getTitle());
      } else {
        log.info(">>> [중복 스킵] 이미 존재하는 URL입니다: {}", article.getSourceUrl());
      }
    }
  }

}