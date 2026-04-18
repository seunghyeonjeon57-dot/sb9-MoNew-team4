package com.example.monew.batch;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
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

  private final NewsCollector newsCollector;
  private final ArticleRepository articleRepository;

  @Value("${news.rss.hankyung}")
  private String hankyungUrl;

  @Value("${news.rss.chosun}")
  private String chosunUrl;

  @Value("${news.rss.yonhap}")
  private String yonhapUrl;
  @Scheduled(cron = "0 0 * * * *") // 매 정각 실행 설정
  @Transactional
  public void runNewsBatch() {
    log.info("뉴스 수집 배치 시작");

    List<ArticleEntity> candidates = new ArrayList<>();
    candidates.addAll(newsCollector.fetchNaver("네이버"));
    candidates.addAll(newsCollector.fetchRss(hankyungUrl, "한국경제", "경제"));
    candidates.addAll(newsCollector.fetchRss(chosunUrl, "조선일보", "사회"));
    candidates.addAll(newsCollector.fetchRss(yonhapUrl, "연합뉴스", "뉴스"));

    int savedCount = 0;
    for (ArticleEntity article : candidates) {
      if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
        articleRepository.save(article);
        savedCount++;
      }
    }

    log.info("배치 종료: {}건 신규 저장 완료", savedCount);
  }
}