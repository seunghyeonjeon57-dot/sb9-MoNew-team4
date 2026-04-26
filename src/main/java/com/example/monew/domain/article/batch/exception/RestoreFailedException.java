package com.example.monew.domain.article.batch.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class RestoreFailedException extends MonewException {
  public RestoreFailedException(String message) {
    super(ErrorCode.BATCH_RESTORE_FAILED, Map.of("message", message));
  }
  public RestoreFailedException(String message, Throwable cause) {
    super(ErrorCode.BATCH_RESTORE_FAILED, Map.of(
        "message", message,
        "cause", cause.getMessage()
    ));
  }
}