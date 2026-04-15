package com.example.monew.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ErrorResponseTest {

  @Test
  @DisplayName("of(ErrorCode): code=enum이름, message=enum.message, status=enum.value, details 빈 맵")
  void ofErrorCode() {
    ErrorResponse response = ErrorResponse.of(ErrorCode.INTEREST_NOT_FOUND);

    assertThat(response.getCode()).isEqualTo("INTEREST_NOT_FOUND");
    assertThat(response.getMessage()).isEqualTo(ErrorCode.INTEREST_NOT_FOUND.getMessage());
    assertThat(response.getStatus()).isEqualTo(ErrorCode.INTEREST_NOT_FOUND.getStatus().value());
    assertThat(response.getDetails()).isEmpty();
    assertThat(response.getTimestamp()).isNotNull();
  }

  @Test
  @DisplayName("of(ErrorCode, details): details가 그대로 채워진다")
  void ofErrorCodeWithDetails() {
    Map<String, Object> details = Map.of("interestId", "abc");

    ErrorResponse response = ErrorResponse.of(ErrorCode.INTEREST_NOT_FOUND, details);

    assertThat(response.getDetails()).containsEntry("interestId", "abc");
    assertThat(response.getCode()).isEqualTo("INTEREST_NOT_FOUND");
  }

  @Test
  @DisplayName("of(MonewException) 헬퍼: code/status/details를 예외에서 채워준다")
  void ofMonewException() {
    MonewException ex = new MonewException(ErrorCode.SIMILAR_INTEREST_NAME,
        Map.of("existing", "AI")) {};

    ErrorResponse response = ErrorResponse.of(ex);

    assertThat(response.getCode()).isEqualTo("SIMILAR_INTEREST_NAME");
    assertThat(response.getStatus()).isEqualTo(ErrorCode.SIMILAR_INTEREST_NAME.getStatus().value());
    assertThat(response.getDetails()).containsEntry("existing", "AI");
  }
}
