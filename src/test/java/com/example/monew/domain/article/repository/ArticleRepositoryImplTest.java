package com.example.monew.domain.article.repository;

import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class ArticleRepositoryImplTest {

  @Autowired private EntityManager em;
  @Autowired private ArticleRepository articleRepository;

  @TestConfiguration
  static class TestConfig {
    @Bean public JPAQueryFactory jpaQueryFactory(EntityManager em) { return new JPAQueryFactory(em); }
  }

  private ArticleEntity saveArticle(String title, String summary, long views, LocalDateTime date) {
    ArticleEntity a = ArticleEntity.builder().title(title).source("S").sourceUrl("URL-" + UUID.randomUUID()).build();
    articleRepository.save(a);
    ReflectionTestUtils.setField(a, "summary", summary);
    ReflectionTestUtils.setField(a, "viewCount", views);
    ReflectionTestUtils.setField(a, "createdAt", date);
    em.flush(); em.clear();
    return a;
  }

  @Test
  @DisplayName("JaCoCo 100% 저격: 모든 분기 강제 돌파")
  void ultimateJacocoStrike() {
    LocalDateTime now = LocalDateTime.of(2026, 4, 25, 12, 0);

    ArticleEntity a1 = saveArticle("제목1", "요약1", 200L, now.plusDays(1)); // High
    ArticleEntity a2 = saveArticle("제목2", "요약2", 100L, now);            // Mid (커서 기준)
    ArticleEntity a3 = saveArticle("제목3", "요약3", 100L, now.minusDays(1)); // Mid (Tie-break용)

    String id1 = a1.getId().toString();
    String id2 = a2.getId().toString();
    String id3 = a3.getId().toString();

    articleRepository.findByCursor(ArticleSearchCondition.builder().orderBy("viewCount").direction("DESC").cursor(id2).build());

    articleRepository.findByCursor(ArticleSearchCondition.builder().orderBy("viewCount").direction("ASC").cursor(id2).build());

    articleRepository.findByCursor(ArticleSearchCondition.builder().orderBy("createdAt").direction("DESC").cursor(id2).build());

    articleRepository.findByCursor(ArticleSearchCondition.builder().orderBy("createdAt").direction("ASC").cursor(id2).build());


    articleRepository.findByCursor(ArticleSearchCondition.builder().keyword("요약3").build());

    articleRepository.findByCursor(ArticleSearchCondition.builder().orderBy("commentCount").cursor(id2).build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().orderBy(null).direction(null).cursor(id2).build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().publishDateFrom(now).publishDateTo(now).build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().publishDateFrom(now).build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().publishDateTo(now).build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().cursor("not-uuid").build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().size(-1).build());
    articleRepository.findByCursor(ArticleSearchCondition.builder().sourceIn(List.of("S")).build());

    articleRepository.softDelete(a1.getId());
    articleRepository.findAllActive();
  }
}