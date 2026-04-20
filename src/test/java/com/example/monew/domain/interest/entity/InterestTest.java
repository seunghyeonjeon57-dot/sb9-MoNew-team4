package com.example.monew.domain.interest.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InterestTest {

  @Test
  @DisplayName("name + keywords로 생성: 키워드 리스트가 보존되고, isDeleted=false, subscriberCount=0")
  void createWithNameAndKeywords() {
    Interest interest = Interest.builder()
        .name("인공지능")
        .keywords(List.of("AI", "ML"))
        .build();

    assertThat(interest.getName()).isEqualTo("인공지능");
    assertThat(interest.getKeywords()).extracting(InterestKeyword::getValue)
        .containsExactly("AI", "ML");
    assertThat(interest.isDeleted()).isFalse();
    assertThat(interest.getSubscriberCount()).isZero();
  }

  @Test
  @DisplayName("replaceKeywords: 기존 키워드를 비우고 새 리스트로 교체")
  void replaceKeywordsClearsAndAppends() {
    Interest interest = Interest.builder()
        .name("인공지능")
        .keywords(List.of("AI"))
        .build();

    interest.replaceKeywords(List.of("ML", "DL"));

    assertThat(interest.getKeywords()).extracting(InterestKeyword::getValue)
        .containsExactly("ML", "DL");
  }

  @Test
  @DisplayName("markDeleted: isDeleted=true 전환")
  void markDeletedFlipsFlag() {
    Interest interest = Interest.builder()
        .name("인공지능")
        .keywords(List.of("AI"))
        .build();

    interest.markDeleted();

    assertThat(interest.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("name이 blank이면 IllegalArgumentException")
  void blankNameRejected() {
    assertThatThrownBy(() -> new Interest(" ", List.of("AI")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("keywords가 비어 있으면 IllegalArgumentException")
  void emptyKeywordsRejected() {
    assertThatThrownBy(() -> new Interest("인공지능", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  // 아래 세 테스트는 생성자 검증 테스트(blankNameRejected / emptyKeywordsRejected)와
  // 경로가 동일하지만, @Builder 가 생성자 레벨을 벗어나 클래스 레벨로 이동할 경우
  // 검증 우회 경로가 생기는 회귀를 잡기 위한 안전장치로 유지한다.
  @Test
  @DisplayName("빌더 — name이 blank이면 IllegalArgumentException")
  void builderBlankNameRejected() {
    assertThatThrownBy(() -> Interest.builder().name(" ").keywords(List.of("AI")).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("빌더 — keywords가 비어 있으면 IllegalArgumentException")
  void builderEmptyKeywordsRejected() {
    assertThatThrownBy(() -> Interest.builder().name("인공지능").keywords(List.of()).build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("빌더 — keywords가 null이면 IllegalArgumentException")
  void builderNullKeywordsRejected() {
    assertThatThrownBy(() -> Interest.builder().name("인공지능").keywords(null).build())
        .isInstanceOf(IllegalArgumentException.class);
  }
}
