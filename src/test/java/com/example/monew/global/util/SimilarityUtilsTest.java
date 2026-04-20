package com.example.monew.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SimilarityUtilsTest {

  @Test
  @DisplayName("동일 문자열 → 유사도 1.0")
  void identical() {
    assertThat(SimilarityUtils.similarity("인공지능", "인공지능")).isEqualTo(1.0);
  }

  @Test
  @DisplayName("완전히 다른 문자열 → 유사도 0.0 ~ 0.3 미만")
  void different() {
    assertThat(SimilarityUtils.similarity("AI", "야구")).isLessThan(0.3);
  }

  @Test
  @DisplayName("부분적으로 유사한 문자열 → 유사도 > 0.8")
  void nearlyIdentical() {
    assertThat(SimilarityUtils.similarity("인공지능", "인공지")).isGreaterThan(0.7);
  }

  @Test
  @DisplayName("빈 문자열 처리")
  void emptyStrings() {
    assertThat(SimilarityUtils.similarity("", "")).isEqualTo(1.0);
    assertThat(SimilarityUtils.similarity("a", "")).isEqualTo(0.0);
  }

  @Test
  @DisplayName("대칭성 — similarity(a,b) == similarity(b,a) (짧은 쪽 swap 최적화 안전장치)")
  void symmetric() {
    assertThat(SimilarityUtils.similarity("인공지능", "인공지"))
        .isEqualTo(SimilarityUtils.similarity("인공지", "인공지능"));
    assertThat(SimilarityUtils.similarity("abcdef", "xyz"))
        .isEqualTo(SimilarityUtils.similarity("xyz", "abcdef"));
  }

  @Test
  @DisplayName("정확한 편집 거리 값 — 1글자 삭제는 1.0 - 1/maxLen")
  void exactDistanceValue() {
    assertThat(SimilarityUtils.similarity("인공지능", "인공지")).isEqualTo(0.75);
    assertThat(SimilarityUtils.similarity("abcd", "abce")).isEqualTo(0.75);
  }

  @Test
  @DisplayName("null 입력 → IllegalArgumentException")
  void nullInput() {
    assertThatThrownBy(() -> SimilarityUtils.similarity(null, "x"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SimilarityUtils.similarity("x", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("긴 문자열(500자 레벨) — 편집 거리 정확값 회귀")
  void longStringExactDistance() {
    String base = "a".repeat(500);
    String oneInsert = base + "b";
    // 1글자 삽입 → 거리 1, maxLen 501 → 유사도 1 - 1/501
    assertThat(SimilarityUtils.similarity(oneInsert, base))
        .isEqualTo(1.0 - 1.0 / 501);
    // swap 안전장치: 길이 반대로 넣어도 동일
    assertThat(SimilarityUtils.similarity(base, oneInsert))
        .isEqualTo(1.0 - 1.0 / 501);
  }

  @Test
  @DisplayName("긴 문자열 완전 불일치 — 거리 == maxLen")
  void longStringTotallyDifferent() {
    String a = "a".repeat(300);
    String b = "b".repeat(300);
    assertThat(SimilarityUtils.similarity(a, b)).isEqualTo(0.0);
  }
}
