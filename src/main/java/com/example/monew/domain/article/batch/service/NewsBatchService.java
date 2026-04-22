package com.example.monew.domain.article.batch.service;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.article.repository.ArticleRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class NewsBatchService {

  private final ArticleRepository articleRepository;

  @Transactional
  public void saveIfUnique(List<ArticleEntity> articles) {
    for (ArticleEntity article : articles) {
      if (!articleRepository.existsBySourceUrl(article.getSourceUrl())) {
        articleRepository.saveAndFlush(article); // save 대신 saveAndFlush 시도
        log.info(">>> [진짜 DB 전송] {}", article.getTitle());
      }
    }
  }
}
