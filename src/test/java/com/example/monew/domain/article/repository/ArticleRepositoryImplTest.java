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
  @Autowired
  private jakarta.persistence.EntityManager em;

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

  @Test
  @DisplayName("커서 테스트 커버리지 반영")
  void findByCursorTest() {
    LocalDateTime sameTime = LocalDateTime.now().withNano(0);
    LocalDateTime yesterday = sameTime.minusDays(1);

    ArticleEntity older = articleRepository.save(ArticleEntity.builder()
        .title("과거").source("A").sourceUrl("U1").build());
    ArticleEntity latest1 = articleRepository.save(ArticleEntity.builder()
        .title("최신1").source("B").sourceUrl("U2").build());
    ArticleEntity latest2 = articleRepository.save(ArticleEntity.builder()
        .title("최신2").source("C").sourceUrl("U3").build());

    org.springframework.test.util.ReflectionTestUtils.setField(older, "createdAt", yesterday);
    org.springframework.test.util.ReflectionTestUtils.setField(latest1, "createdAt", sameTime);
    org.springframework.test.util.ReflectionTestUtils.setField(latest2, "createdAt", sameTime);

    em.flush();
    em.clear();

    ArticleEntity biggerIdArticle =
        (latest1.getId().compareTo(latest2.getId()) > 0) ? latest1 : latest2;
    ArticleEntity smallerIdArticle = (biggerIdArticle == latest1) ? latest2 : latest1;

    var results = articleRepository.findByCursor(biggerIdArticle.getId(), sameTime, 10);

    assertThat(results)
        .extracting(ArticleEntity::getTitle)
        .contains(smallerIdArticle.getTitle(), "과거");

    assertThat(results).hasSize(2);
  }
}
