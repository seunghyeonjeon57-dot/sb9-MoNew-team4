package com.example.monew.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionTest {

  private final GlobalException handler = new GlobalException();

  static class FixtureException extends MonewException {
    FixtureException(ErrorCode code, Map<String, Object> details) {
      super(code, details);
    }
  }

  @Test
  @DisplayName("handleMonewException: ErrorCode.status 그대로, body는 ErrorResponse")
  void handleMonewExceptionMapsStatusFromErrorCode() {
    FixtureException ex = new FixtureException(ErrorCode.SIMILAR_INTEREST_NAME,
        Map.of("existing", "AI"));

    ResponseEntity<ErrorResponse> response = handler.handleMonewException(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("SIMILAR_INTEREST_NAME");
    assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    assertThat(response.getBody().getDetails()).containsEntry("existing", "AI");
  }

  @Test
  @DisplayName("handleException(fallback): INTERNAL_SERVER_ERROR + 예외 클래스명 노출")
  void handleFallback() {
    ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo(500);
  }
}
