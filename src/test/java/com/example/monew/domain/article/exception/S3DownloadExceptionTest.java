package com.example.monew.domain.article.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monew.domain.article.batch.exception.S3DownloadException;
import com.example.monew.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;

class S3DownloadExceptionTest {

  @Test
  @DisplayName("S3 다운로드 실패 예외 생성 시 에러 코드와 상세 정보 확인")
  void constructorTest() {
    String key = "test-article.json";
    Throwable cause = new RuntimeException("Network Timeout");

    S3DownloadException exception = new S3DownloadException(key, cause);

    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.S3_DOWNLOAD_FAILED);

    assertThat(exception.getMessage()).isEqualTo(ErrorCode.S3_DOWNLOAD_FAILED.getMessage());

    Map<String, Object> details = exception.getDetails();
    assertThat(details.get("key")).isEqualTo(key);
    assertThat(details.get("reason")).isEqualTo("Network Timeout");
  }
}