package com.example.monew.domain.article.exception;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.example.monew.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ExceptionTest {

  @Test
  @DisplayName("커스텀 예외 생성 시 설정한 ErrorCode가 올바르게 저장되어야 한다")
  void exceptionCoverageTest() {

    ArticleNotFoundException ex1 = new ArticleNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
    assertThat(ex1.getErrorCode()).isEqualTo(ErrorCode.ARTICLE_NOT_FOUND);

    BackupFileNotFoundException ex2 = new BackupFileNotFoundException(ErrorCode.ARTICLE_NOT_FOUND);
    assertThat(ex2.getErrorCode()).isEqualTo(ErrorCode.ARTICLE_NOT_FOUND);

    InvalidCursorException ex3 = new InvalidCursorException(ErrorCode.INVALID_INPUT_VALUE);
    assertThat(ex3.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

    InvalidRestoreDateException ex4 = new InvalidRestoreDateException(ErrorCode.INVALID_INPUT_VALUE);
    assertThat(ex4.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }
}
