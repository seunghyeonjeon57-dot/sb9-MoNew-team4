package com.example.monew.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SimilarityUtilsTest {

  @Nested
  @DisplayName("similarity(a, b)는 Levenshtein 거리 기반 비율을 반환한다")
  class Similarity {

    @Test
    @DisplayName("완전 동일한 문자열은 1.0")
    void identicalReturnsOne() {
      assertThat(SimilarityUtils.similarity("인공지능", "인공지능")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("완전히 다른 문자열은 0에 가깝다")
    void completelyDifferent() {
      assertThat(SimilarityUtils.similarity("ABCD", "WXYZ")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("둘 다 빈 문자열이면 1.0")
    void bothEmptyReturnsOne() {
      assertThat(SimilarityUtils.similarity("", "")).isEqualTo(1.0);
    }

    @Test
    @DisplayName("한쪽이 null이면 0.0")
    void nullReturnsZero() {
      assertThat(SimilarityUtils.similarity(null, "a")).isEqualTo(0.0);
      assertThat(SimilarityUtils.similarity("a", null)).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("isSimilar 경계값: 80% 임계값")
  class Threshold {

    @Test
    @DisplayName("경계값 80% 이상: 임계값 0.80 이상이면 유사로 판정")
    void aboveOrEqualThresholdIsSimilar() {
      // 5글자 기준 4/5=0.80
      assertThat(SimilarityUtils.similarity("abcde", "abcdf")).isGreaterThanOrEqualTo(0.80);
      assertThat(SimilarityUtils.isSimilar("abcde", "abcdf")).isTrue();
    }

    @Test
    @DisplayName("경계값 미만: 임계값 0.80 미만이면 유사 아님")
    void belowThresholdIsNotSimilar() {
      // 3/5=0.6
      assertThat(SimilarityUtils.similarity("abcde", "abcxy")).isLessThan(0.80);
      assertThat(SimilarityUtils.isSimilar("abcde", "abcxy")).isFalse();
    }

    @Test
    @DisplayName("임계값 상수는 0.80")
    void thresholdConstantIs080() {
      assertThat(SimilarityUtils.SIMILAR_THRESHOLD).isEqualTo(0.80);
    }
  }
}
