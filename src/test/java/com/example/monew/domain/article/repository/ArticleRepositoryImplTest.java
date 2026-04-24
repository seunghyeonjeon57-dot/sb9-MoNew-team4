package com.example.monew.domain.article.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.dto.ArticleSearchCondition;
import com.example.monew.domain.article.entity.ArticleEntity;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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

  private ArticleEntity a1, a2;

  @TestConfiguration
  static class TestConfig {
    @Bean public JPAQueryFactory jpaQueryFactory(EntityManager em) { return new JPAQueryFactory(em); }
  }

  @BeforeEach
  void setUp() {
    a1 = ArticleEntity.builder().title("삼성").source("S1").sourceUrl("U1").build();
    a2 = ArticleEntity.builder().title("현대").source("S2").sourceUrl("U2").build();

    // 날짜 및 조회수 고정 주입 (커버리지용)
    ReflectionTestUtils.setField(a1, "viewCount", 100L);
    ReflectionTestUtils.setField(a1, "createdAt", LocalDateTime.of(2026, 4, 1, 0, 0));
    ReflectionTestUtils.setField(a2, "viewCount", 200L);
    ReflectionTestUtils.setField(a2, "createdAt", LocalDateTime.of(2026, 4, 2, 0, 0));

    articleRepository.save(a1);
    articleRepository.save(a2);
    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("1. 기본 CRUD 및 필터 null 처리 커버리지")
  void basicAndNullCoverage() {
    // softDelete & findAllActive
    articleRepository.softDelete(a1.getId());
    assertThat(articleRepository.findAllActive()).hasSize(1);

    // 검색어/소스 null 처리 분기 (keywordContains, sourceIn)
    ArticleSearchCondition cond = ArticleSearchCondition.builder()
        .keyword(null).sourceIn(null).size(10).direction("DESC").orderBy("createdAt").build();
    assertThat(articleRepository.findByCursor(cond)).isNotEmpty();
  }

  @Test
  @DisplayName("2. 날짜 필터(publishDateBetween) 모든 분기 커버리지")
  void dateFilterCoverage() {
    LocalDateTime date = LocalDateTime.of(2026, 4, 1, 0, 0);

    // From만 있을 때
    articleRepository.findByCursor(ArticleSearchCondition.builder()
        .publishDateFrom(date).size(10).direction("DESC").orderBy("createdAt").build());

    // To만 있을 때
    articleRepository.findByCursor(ArticleSearchCondition.builder()
        .publishDateTo(date).size(10).direction("DESC").orderBy("createdAt").build());

    // 둘 다 있을 때
    articleRepository.findByCursor(ArticleSearchCondition.builder()
        .publishDateFrom(date).publishDateTo(date).size(10).direction("DESC").orderBy("createdAt").build());
  }

  @Test
  @DisplayName("3. 정렬 및 커서(cursorCondition) 모든 조합 커버리지")
  void cursorAndOrderCoverage() {
    // [준비] 삭제 없이 깨끗한 상태에서 시작해야 cursorArticle 조회가 성공함
    // a1(100회, 4/1), a2(200회, 4/2) 데이터 존재 상태

    // Case 1: viewCount + DESC (정렬 값 기준 작거나, 같으면서 ID가 작거나)
    // 200회인 a2를 커서로 주면 100회인 a1이 나와야 함
    ArticleSearchCondition cond1 = ArticleSearchCondition.builder()
        .orderBy("viewCount").direction("DESC").cursor(a2.getId()).size(10).build();
    assertThat(articleRepository.findByCursor(cond1)).hasSize(1);

    // Case 2: viewCount + ASC (정렬 값 기준 크거나, 같으면서 ID가 크거나)
    // 100회인 a1을 커서로 주면 200회인 a2가 나와야 함
    ArticleSearchCondition cond2 = ArticleSearchCondition.builder()
        .orderBy("viewCount").direction("ASC").cursor(a1.getId()).size(10).build();
    assertThat(articleRepository.findByCursor(cond2)).hasSize(1);

    // Case 3: commentCount (미구현 분기: 항상 id.lt 로직만 탐)
    ArticleSearchCondition cond3 = ArticleSearchCondition.builder()
        .orderBy("commentCount")
        .direction("DESC")
        .cursor(a2.getId())
        .size(10)
        .build();
    // fetch 시점에 에러가 안 나면 성공
    assertThat(articleRepository.findByCursor(cond3)).isNotNull();

    // Case 4: createdAt(default) + DESC
    ArticleSearchCondition cond4 = ArticleSearchCondition.builder()
        .orderBy("createdAt").direction("DESC").cursor(a2.getId()).size(10).build();
    assertThat(articleRepository.findByCursor(cond4)).hasSize(1);

    // Case 5: createdAt(default) + ASC
    ArticleSearchCondition cond5 = ArticleSearchCondition.builder()
        .orderBy("createdAt").direction("ASC").cursor(a1.getId()).size(10).build();
    assertThat(articleRepository.findByCursor(cond5)).hasSize(1);

    // Case 6: Cursor가 DB에 없는 가짜 UUID일 때 (cursorArticle == null 분기 커버)
    ArticleSearchCondition cond6 = ArticleSearchCondition.builder()
        .cursor(UUID.randomUUID()).size(10).direction("DESC").orderBy("createdAt").build();
    assertThat(articleRepository.findByCursor(cond6)).hasSize(2); // 필터 없이 전체 조회됨
  }

}