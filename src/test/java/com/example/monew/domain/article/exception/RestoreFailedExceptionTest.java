package com.example.monew.domain.article.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestoreFailedExceptionTest {

  @Test
  @DisplayName("메시지만 전달하여 예외를 생성")
  void constructorWithMessageTest() {
    String message = "복구 실패 테스트";
    RestoreFailedException exception = new RestoreFailedException(message);

    assertThat(exception.getMessage()).isEqualTo(message);
  }

  @Test
  @DisplayName("메시지와 원인 예외를 함께 전달하여 생성")
  void constructorWithMessageAndCauseTest() {
    String message = "복구 실패 및 원인 포함";
    Throwable cause = new RuntimeException("원인 에러");

    RestoreFailedException exception = new RestoreFailedException(message, cause);

    assertThat(exception.getMessage()).isEqualTo(message);
    assertThat(exception.getCause()).isEqualTo(cause);
  }
}
