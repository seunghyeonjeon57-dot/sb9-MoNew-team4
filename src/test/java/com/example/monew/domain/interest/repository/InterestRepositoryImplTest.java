package com.example.monew.domain.interest.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.config.JpaAuditConfig;
import com.example.monew.config.QueryDslTestConfig;
import com.example.monew.domain.interest.entity.Interest;
import com.example.monew.domain.interest.repository.InterestRepositoryCustom.CursorPage;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({QueryDslTestConfig.class, JpaAuditConfig.class, InterestRepositoryImpl.class})
class InterestRepositoryImplTest {

  @Autowired
  private InterestRepository interestRepository;

  @Autowired
  private TestEntityManager em;

  private Interest seed(String name, List<String> keywords) {
    return interestRepository.saveAndFlush(
        Interest.builder().name(name).keywords(keywords).build());
  }

  @Test
  @DisplayName("findByCursor: 첫 페이지 (cursor=null) name ASC 정렬로 상위 limit 건 반환 + hasNext 판정")
  void findByCursor_firstPage_orderByName_asc() {
    seed("가경제", List.of("주식"));
    seed("나스포츠", List.of("축구"));
    seed("다문화", List.of("영화"));
    seed("라과학", List.of("우주"));
    seed("마여행", List.of("유럽"));
    em.flush();
    em.clear();

    CursorPage page = interestRepository.findByCursor(null, "name", "ASC", null, 3);

    assertThat(page.content()).extracting(Interest::getName)
        .containsExactly("가경제", "나스포츠", "다문화");
    assertThat(page.hasNext()).isTrue();
    assertThat(page.totalElements()).isEqualTo(5L);
  }

  @Test
  @DisplayName("findByCursor: cursorId 이후 페이지를 name ASC 로 이어서 반환")
  void findByCursor_secondPage_orderByName_asc() {
    Interest a = seed("가경제", List.of("a"));
    Interest b = seed("나스포츠", List.of("b"));
    Interest c = seed("다문화", List.of("c"));
    Interest d = seed("라과학", List.of("d"));
    Interest e = seed("마여행", List.of("e"));
    em.flush();
    em.clear();

    CursorPage page = interestRepository.findByCursor(null, "name", "ASC", c.getId(), 10);

    assertThat(page.content()).extracting(Interest::getName)
        .containsExactly("라과학", "마여행");
    assertThat(page.hasNext()).isFalse();
    assertThat(page.totalElements()).isEqualTo(5L);
  }

  @Test
  @DisplayName("findByCursor: subscriberCount DESC — 인기 순 정렬 + 첫 페이지")
  void findByCursor_orderBySubscriberCount_desc() {
    Interest low = seed("낮은구독", List.of("a"));
    Interest mid = seed("중간구독", List.of("b"));
    Interest high = seed("높은구독", List.of("c"));
    // high=3, mid=2, low=1
    interestRepository.incrementSubscriberCount(high.getId());
    interestRepository.incrementSubscriberCount(high.getId());
    interestRepository.incrementSubscriberCount(high.getId());
    interestRepository.incrementSubscriberCount(mid.getId());
    interestRepository.incrementSubscriberCount(mid.getId());
    interestRepository.incrementSubscriberCount(low.getId());
    em.flush();
    em.clear();

    CursorPage page = interestRepository.findByCursor(
        null, "subscriberCount", "DESC", null, 10);

    assertThat(page.content()).extracting(Interest::getName)
        .containsExactly("높은구독", "중간구독", "낮은구독");
    assertThat(page.hasNext()).isFalse();
  }

  @Test
  @DisplayName("findByCursor: keyword 로 이름 부분 일치 (대소문자 무시)")
  void findByCursor_keywordFilter_byName() {
    seed("경제뉴스", List.of("a"));
    seed("스포츠뉴스", List.of("b"));
    seed("경제분석", List.of("c"));
    em.flush();
    em.clear();

    CursorPage page = interestRepository.findByCursor(
        "경제", "name", "ASC", null, 10);

    assertThat(page.content()).extracting(Interest::getName)
        .containsExactlyInAnyOrder("경제뉴스", "경제분석");
    assertThat(page.totalElements()).isEqualTo(2L);
  }

  @Test
  @DisplayName("findByCursor: keyword 로 interest_keywords.value 부분 일치")
  void findByCursor_keywordFilter_byKeywordValue() {
    seed("도메인X", List.of("인공지능", "머신러닝"));
    seed("도메인Y", List.of("여행", "유럽"));
    seed("도메인Z", List.of("기계", "로봇"));
    em.flush();
    em.clear();

    CursorPage page = interestRepository.findByCursor(
        "머신", "name", "ASC", null, 10);

    assertThat(page.content()).extracting(Interest::getName)
        .containsExactly("도메인X");
    assertThat(page.totalElements()).isEqualTo(1L);
  }

  @Test
  @DisplayName("findByCursor: subscriberCount 동률 시 createdAt/id tie-breaker 로 안정된 순서 반환")
  void findByCursor_tieBreaker_sameSubscriberCount() {
    Interest a = seed("첫번째", List.of("a"));
    Interest b = seed("두번째", List.of("b"));
    Interest c = seed("세번째", List.of("c"));
    // 모두 subscriberCount=1 로 동률
    interestRepository.incrementSubscriberCount(a.getId());
    interestRepository.incrementSubscriberCount(b.getId());
    interestRepository.incrementSubscriberCount(c.getId());
    em.flush();
    em.clear();

    CursorPage first = interestRepository.findByCursor(
        null, "subscriberCount", "ASC", null, 2);
    CursorPage second = interestRepository.findByCursor(
        null, "subscriberCount", "ASC",
        first.content().get(first.content().size() - 1).getId(), 2);

    assertThat(first.content()).hasSize(2);
    assertThat(first.hasNext()).isTrue();
    assertThat(second.content()).hasSize(1);
    assertThat(second.hasNext()).isFalse();

    // 첫 페이지 + 둘째 페이지를 합친 결과가 시드 순서와 일치 (createdAt ASC, id ASC tie-breaker)
    assertThat(first.content()).extracting(Interest::getName)
        .containsExactly("첫번째", "두번째");
    assertThat(second.content()).extracting(Interest::getName)
        .containsExactly("세번째");
  }
}
