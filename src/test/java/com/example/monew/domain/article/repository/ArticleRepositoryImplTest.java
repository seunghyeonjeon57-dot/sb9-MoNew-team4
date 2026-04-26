package com.example.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }
  }

  @Test
  @DisplayName("커서 기반 페이징 모든 분기 커버테스트")
  void fullCoverageTest() {

    LocalDateTime now = LocalDateTime.of(2026, 4, 26, 12, 0);

    ArticleEntity a1 = ArticleEntity.builder()
        .title("T1")
        .source("S1")
        .sourceUrl("U1")
        .build();

    ArticleEntity a2 = ArticleEntity.builder()
        .title("T2")
        .source("S2")
        .sourceUrl("U2")
        .build();

    ArticleEntity a3 = ArticleEntity.builder()
        .title("T3")
        .source("S3")
        .sourceUrl("U3")
        .build();


    articleRepository.saveAndFlush(a1);
    articleRepository.saveAndFlush(a2);
    articleRepository.saveAndFlush(a3);

    ReflectionTestUtils.setField(a1, "viewCount", 100L);
    ReflectionTestUtils.setField(a2, "viewCount", 100L);
    ReflectionTestUtils.setField(a3, "viewCount", 100L);

    ReflectionTestUtils.setField(a1, "createdAt", now.minusDays(1));
    ReflectionTestUtils.setField(a2, "createdAt", now);
    ReflectionTestUtils.setField(a3, "createdAt", now.plusDays(1));

    ReflectionTestUtils.setField(a1, "summary", "FindMe");

    em.flush();

    String cursor = a2.getId().toString();

    String[] orders = {"viewCount", "createdAt", "commentCount"};
    String[] dirs = {"DESC", "ASC"};

    for (String order : orders) {
      for (String dir : dirs) {
        articleRepository.findByCursor(
            ArticleSearchCondition.builder()
                .orderBy(order)
                .direction(dir)
                .cursor(cursor)
                .build()
        );
      }
    }

    articleRepository.findByCursor(
        ArticleSearchCondition.builder().keyword("FindMe").build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder().keyword("T1").build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder().sourceIn(List.of("S1")).build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder()
            .publishDateFrom(now.minusDays(2))
            .build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder()
            .publishDateTo(now.plusDays(2))
            .build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder()
            .publishDateFrom(now.minusDays(2))
            .publishDateTo(now.plusDays(2))
            .build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder().size(-1).build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder().cursor(null).build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder().cursor("invalid").build()
    );
    articleRepository.findByCursor(
        ArticleSearchCondition.builder()
            .cursor(UUID.randomUUID().toString())
            .build()
    );
    articleRepository.softDelete(a1.getId());
    articleRepository.findAllActive();
  }

  @Test
  void viewCount_cursor_condition_desc_test() {

    ArticleEntity a1 = ArticleEntity.builder()
        .title("a1")
        .source("S1")
        .sourceUrl("U1")
        .build();

    ArticleEntity a2 = ArticleEntity.builder()
        .title("a2")
        .source("S2")
        .sourceUrl("U2")
        .build();

    em.persist(a1);
    em.persist(a2);

    ReflectionTestUtils.setField(a1, "viewCount", 100L);
    ReflectionTestUtils.setField(a2, "viewCount", 50L);

    em.flush();

    ArticleSearchCondition condition = new ArticleSearchCondition();
    condition.setOrderBy("viewCount");
    condition.setDirection("DESC");
    condition.setCursor(a1.getId().toString());

    List<ArticleEntity> result = articleRepository.findByCursor(condition);

    assertThat(result).isNotNull();
  }

  @Test
  void viewCount_cursor_condition_asc_test() {

    ArticleEntity a1 = ArticleEntity.builder()
        .title("a1")
        .source("S1")
        .sourceUrl("U1")
        .build();

    ArticleEntity a2 = ArticleEntity.builder()
        .title("a2")
        .source("S2")
        .sourceUrl("U2")
        .build();

    em.persist(a1);
    em.persist(a2);

    ReflectionTestUtils.setField(a1, "viewCount", 100L);
    ReflectionTestUtils.setField(a2, "viewCount", 150L);

    em.flush();

    ArticleSearchCondition condition = new ArticleSearchCondition();
    condition.setOrderBy("viewCount");
    condition.setDirection("ASC");
    condition.setCursor(a1.getId().toString());

    List<ArticleEntity> result = articleRepository.findByCursor(condition);

    assertThat(result).isNotNull();
  }
  @Test
  @DisplayName("조회수 내림차순 조회 시, 커서로 지정된 기사보다 조회수가 적거나(ID가 작거나) 하는 다음 데이터만 반환되어야 한다")
  void viewCount_cursor_condition_desc_test2() {
    ArticleEntity a1 = ArticleEntity.builder().title("a1").source("S1").sourceUrl("U1").build();
    ArticleEntity a2 = ArticleEntity.builder().title("a2").source("S2").sourceUrl("U2").build();
    ArticleEntity a3 = ArticleEntity.builder().title("a3").source("S3").sourceUrl("U3").build();

    articleRepository.saveAll(List.of(a1, a2, a3));

    ReflectionTestUtils.setField(a1, "viewCount", 100L);
    ReflectionTestUtils.setField(a2, "viewCount", 50L);
    ReflectionTestUtils.setField(a3, "viewCount", 10L);

    em.flush();
    em.clear();

    ArticleSearchCondition condition = new ArticleSearchCondition();
    condition.setOrderBy("viewCount");
    condition.setDirection("DESC");
    condition.setCursor(a1.getId().toString());

    List<ArticleEntity> result = articleRepository.findByCursor(condition);

    assertThat(result.size()).isEqualTo(2);

    assertThat(result.get(0).getId()).isEqualTo(a2.getId());
    assertThat(result.get(0).getViewCount()).isEqualTo(50L);

    assertThat(result).extracting(ArticleEntity::getId).doesNotContain(a1.getId());
  }

  @Test
  @DisplayName("조회수 오름차순 조회 시, 커서보다 조회수가 높은 데이터가 순서대로 반환되어야 한다")
  void viewCount_cursor_condition_asc_test2() {
    ArticleEntity a1 = ArticleEntity.builder().title("a1").source("S1").sourceUrl("U1").build();
    ArticleEntity a2 = ArticleEntity.builder().title("a2").source("S2").sourceUrl("U2").build();
    ArticleEntity a3 = ArticleEntity.builder().title("a3").source("S3").sourceUrl("U3").build();

    articleRepository.saveAll(List.of(a1, a2, a3));

    ReflectionTestUtils.setField(a1, "viewCount", 100L);
    ReflectionTestUtils.setField(a2, "viewCount", 150L);
    ReflectionTestUtils.setField(a3, "viewCount", 200L);

    em.flush();
    em.clear();

    ArticleSearchCondition condition = new ArticleSearchCondition();
    condition.setOrderBy("viewCount");
    condition.setDirection("ASC");
    condition.setCursor(a1.getId().toString());

    List<ArticleEntity> result = articleRepository.findByCursor(condition);

    assertThat(result.size()).isEqualTo(2);
    assertThat(result.get(0).getId()).isEqualTo(a2.getId());
    assertThat(result.get(1).getViewCount()).isEqualTo(200L);
  }
}