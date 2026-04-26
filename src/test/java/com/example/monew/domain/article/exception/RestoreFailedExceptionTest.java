package com.example.monew.domain.article.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.exception.RestoreFailedException;
import com.example.monew.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestoreFailedExceptionTest {

  @Test
  @DisplayName("메시지만 전달하여 예외를 생성")
  void constructorWithMessageTest() {
    String message = "복구 실패 테스트";
    RestoreFailedException exception = new RestoreFailedException(message);
    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BATCH_RESTORE_FAILED);
  }
}
