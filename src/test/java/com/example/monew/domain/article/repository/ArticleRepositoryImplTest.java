package com.example.monew.domain.article.repository;


import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.config.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("softDelete 후 findAllActive에서 제외되고, findByCursor도 결과에서 제외되어야 한다")
  void softDelete() {
    ArticleEntity saved = articleRepository.save(ArticleEntity.builder()
        .title("테스트 뉴스")
        .source("네이버")
        .sourceUrl("https://news.naver.com/test")
        .build());
    UUID id = saved.getId();

    articleRepository.softDelete(id);

    assertThat(articleRepository.findAllActive())
        .extracting(ArticleEntity::getId)
        .doesNotContain(id);

    assertThat(articleRepository.findByCursor(null, null, 10))
        .extracting(ArticleEntity::getId)
        .doesNotContain(id);
  }
}