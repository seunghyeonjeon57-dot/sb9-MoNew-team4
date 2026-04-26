package com.example.monew.domain.article.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.exception.S3FileNotFoundException;
import com.example.monew.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class S3FileNotFoundExceptionTest {

  @Test
  @DisplayName("S3 파일 미존재 예외 생성 시 전달한 키 값이 상세 정보에 포함된다")
  void constructorTest() {
    String key = "missing-file.txt";

    S3FileNotFoundException exception = new S3FileNotFoundException(key);

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.S3_FILE_NOT_FOUND);
    assertThat(exception.getMessage()).isEqualTo(ErrorCode.S3_FILE_NOT_FOUND.getMessage());

    assertThat(exception.getDetails().get("key")).isEqualTo(key);
  }
}