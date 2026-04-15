package com.example.monew.global.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MonewExceptionTest {

  static class FixtureException extends MonewException {
    FixtureException() {
      super(ErrorCode.INTEREST_NOT_FOUND);
    }

    FixtureException(Map<String, Object> details) {
      super(ErrorCode.INTEREST_NOT_FOUND, details);
    }
  }

  @Test
  @DisplayName("기본 생성자: details는 빈 맵, message는 ErrorCode.message")
  void defaultDetailsIsEmptyMap() {
    FixtureException ex = new FixtureException();

    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTEREST_NOT_FOUND);
    assertThat(ex.getMessage()).isEqualTo(ErrorCode.INTEREST_NOT_FOUND.getMessage());
    assertThat(ex.getDetails()).isEmpty();
    assertThat(ex.getTimestamp()).isNotNull();
  }

  @Test
  @DisplayName("details 명시 생성자: 전달된 details가 그대로 노출된다")
  void detailsAreExposed() {
    Map<String, Object> details = Map.of("interestId", "abc-123");

    FixtureException ex = new FixtureException(details);

    assertThat(ex.getDetails()).containsEntry("interestId", "abc-123");
  }

  @Test
  @DisplayName("ErrorCode null 입력은 NullPointerException")
  void nullErrorCodeIsRejected() {
    assertThatThrownBy(() -> new MonewException(null) {})
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @DisplayName("MonewException은 abstract — 직접 인스턴스화 불가능 (서브클래스 강제)")
  void monewExceptionIsAbstract() {
    assertThat(java.lang.reflect.Modifier.isAbstract(MonewException.class.getModifiers()))
        .isTrue();
  }
}
