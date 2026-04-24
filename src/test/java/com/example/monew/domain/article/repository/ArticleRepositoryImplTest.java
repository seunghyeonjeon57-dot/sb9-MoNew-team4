package com.example.monew.domain.article.repository;


import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.config.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(QuerydslConfig.class)
class ArticleRepositoryImplTest {

  @Autowired
  private ArticleRepository articleRepository;

  @Test
  void ImplTest() {
    ArticleEntity article = articleRepository.save(ArticleEntity.builder()
        .title("테스트 뉴스")
        .source("네이버")
        .sourceUrl("https://news.naver.com/test")
        .build());
    UUID id = article.getId();

    articleRepository.softDelete(id);

    articleRepository.findAllActive();

    articleRepository.findByCursor(null, null, 10);
    articleRepository.findByCursor(id, null, 10);
    articleRepository.findByCursor(null, LocalDateTime.now(), 10);

    assertThat(id).isNotNull();
  }
}