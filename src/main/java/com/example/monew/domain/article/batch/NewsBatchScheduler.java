package com.example.monew.domain.article.batch;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.service.ArticleService;
import com.example.monew.domain.interest.entity.InterestKeyword;
import com.example.monew.domain.interest.repository.InterestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class NewsBatchScheduler {

  private final NewsCollector newsCollector;
  private final NewsRss newsRss;
  private final InterestRepository interestRepository;
  private final ArticleService articleService;

  @Scheduled(cron = "0 0 * * * *")
  public void runNewsBatch() {
    log.info("=== 뉴스 배치 수집 시작 ===");

    List<String> keywords = interestRepository.findAll().stream()
        .flatMap(interest -> interest.getKeywords().stream())
        .map(InterestKeyword::getValue)
        .distinct()
        .filter(kw -> kw != null && !kw.isBlank())
        .toList();

    if (keywords.isEmpty()) {
      log.warn("수집할 키워드가 없습니다.");
      return;
    }

    for (String keyword : keywords) {
      List<ArticleEntity> naverArticles = newsCollector.fetchNaver(keyword);

      articleService.saveInChunks(naverArticles);

      newsRss.getRss().forEach((pressName, url) -> {
        List<ArticleEntity> rssArticles = newsCollector.fetchRss(url, pressName, keyword);
        articleService.saveInChunks(rssArticles);
      });
    }
    log.info("=== 뉴스 수집 및 저장 완료 ===");
  }
}