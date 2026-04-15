package com.example.monew.global.util;

import static org.assertj.core.api.Assertions.assertThat;

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
}
