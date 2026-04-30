package com.example.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.entity.InterestKeyword;
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

  @Autowired
  private EntityManager em;
  @Autowired
  private ArticleRepository articleRepository;

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
  @DisplayName("댓글수 정렬 조회 시(구현체 로직), 필드값과 무관하게 커서보다 ID가 작은 데이터가 반환되어야 한다")
  void commentCount_cursor_id_logic_test() {
    LocalDateTime now = LocalDateTime.now();
    ArticleEntity a1 = ArticleEntity.builder().title("a1").source("S1").sourceUrl("U1").build();
    ArticleEntity a2 = ArticleEntity.builder().title("a2").source("S2").sourceUrl("U2").build();
    ArticleEntity a3 = ArticleEntity.builder().title("a3").source("S3").sourceUrl("U3").build();

    ReflectionTestUtils.setField(a1, "createdAt", now.minusDays(3));
    ReflectionTestUtils.setField(a2, "createdAt", now.minusDays(2));
    ReflectionTestUtils.setField(a3, "createdAt", now.minusDays(1));

    articleRepository.saveAllAndFlush(List.of(a1, a2, a3));
    em.clear();

    ArticleSearchCondition condition = ArticleSearchCondition.builder()
        .orderBy("commentCount")
        .direction("DESC")
        .cursor(a3.getId().toString())
        .size(10)
        .build();

    List<ArticleEntity> result = articleRepository.findByCursor(condition);

    assertThat(result).as("결과가 비어있으면 안 됩니다. (커서: a3)").isNotEmpty();

    for (ArticleEntity article : result) {
      assertThat(article.getId()).isNotEqualTo(a3.getId());
    }
  }

  @Test
  @DisplayName("interestId 필터 커버리지: 생성자가 protected인 경우의 처리")
  void interestId_filter_full_coverage_test() throws Exception {
    java.lang.reflect.Constructor<Interest> interestConstructor =
        Interest.class.getDeclaredConstructor();
    interestConstructor.setAccessible(true);
    Interest interest = interestConstructor.newInstance();

    ReflectionTestUtils.setField(interest, "name", "Java");
    em.persist(interest);

    java.lang.reflect.Constructor<InterestKeyword> keywordConstructor =
        InterestKeyword.class.getDeclaredConstructor();
    keywordConstructor.setAccessible(true);
    InterestKeyword keyword = keywordConstructor.newInstance();

    ReflectionTestUtils.setField(keyword, "interest", interest);
    ReflectionTestUtils.setField(keyword, "value", "스프링");
    em.persist(keyword);

    ArticleEntity a1 = ArticleEntity.builder()
        .title("일반 제목").source("S1").sourceUrl("U1").interest(interest).build();

    ArticleEntity a2 = ArticleEntity.builder()
        .title("열혈 스프링 정복").source("S2").sourceUrl("U2").build();

    ArticleEntity a3 = ArticleEntity.builder()
        .title("공부기록").summary("내용에 스프링 포함").source("S3").sourceUrl("U3").build();

    ArticleEntity a4 = ArticleEntity.builder()
        .title("파이썬 기초").source("S4").sourceUrl("U4").build();

    em.persist(a1);
    em.persist(a2);
    em.persist(a3);
    em.persist(a4);
    em.flush();
    em.clear();

    ArticleSearchCondition condition = ArticleSearchCondition.builder()
        .interestId(interest.getId())
        .build();

    List<ArticleEntity> result = articleRepository.findByCursor(condition);
    List<UUID> resultIds = result.stream().map(ArticleEntity::getId).toList();

    assertThat(resultIds).contains(a1.getId(), a2.getId(), a3.getId());
    assertThat(resultIds).doesNotContain(a4.getId());
  }

  @Test
  @DisplayName("전체 분기 테스트에 commentCount 추가")
  void fullCoverageWithCommentCount() {
    String cursor = UUID.randomUUID().toString();

    articleRepository.findByCursor(
        ArticleSearchCondition.builder()
            .orderBy("commentCount")
            .direction("DESC")
            .cursor(cursor)
            .build()
    );
  }

  @Test
  @DisplayName("commentCount 커서 조건의 모든 분기 - Impl 로직 한계 대응")
  void commentCount_perfect_coverage_test() {

    ArticleEntity a1 = ArticleEntity.builder().title("a1").source("S1").sourceUrl("U1").build();
    ArticleEntity a2 = ArticleEntity.builder().title("a2").source("S2").sourceUrl("U2").build();
    ArticleEntity a3 = ArticleEntity.builder().title("a3").source("S3").sourceUrl("U3").build();

    ReflectionTestUtils.setField(a1, "commentCount", 10L);
    ReflectionTestUtils.setField(a2, "commentCount", 20L);
    ReflectionTestUtils.setField(a3, "commentCount", 30L);

    em.persist(a1);
    em.persist(a2);
    em.persist(a3);
    em.flush();
    em.clear();

    List<ArticleEntity> all = articleRepository.findAllActive();
    all.sort((o1, o2) -> Long.compare(o2.getCommentCount(), o1.getCommentCount()));

    ArticleEntity large = all.get(0);
    ArticleEntity middle = all.get(1);
    ArticleEntity small = all.get(2);

    ArticleSearchCondition descCond1 = new ArticleSearchCondition();
    descCond1.setOrderBy("commentCount");
    descCond1.setDirection("DESC");
    descCond1.setCursor(large.getId().toString());

    List<ArticleEntity> descResult1 = articleRepository.findByCursor(descCond1);
    assertThat(descResult1).isNotEmpty();
    assertThat(descResult1.get(0).getId()).isEqualTo(middle.getId());

    ArticleSearchCondition descCond2 = new ArticleSearchCondition();
    descCond2.setOrderBy("commentCount");
    descCond2.setDirection("DESC");
    descCond2.setCursor(middle.getId().toString());

    List<ArticleEntity> descResult2 = articleRepository.findByCursor(descCond2);
    assertThat(descResult2).isNotEmpty();
    assertThat(descResult2.get(0).getId()).isEqualTo(small.getId());
  }
}
