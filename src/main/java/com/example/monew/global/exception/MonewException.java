package com.example.monew.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class MonewException extends RuntimeException {
  private final Instant timestamp;
  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  protected MonewException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());

    this.timestamp = Instant.now();
    this.details = details;
    this.errorCode = errorCode;
  }

  protected MonewException(ErrorCode errorCode) {
    this(errorCode, new HashMap<>());
  }
}
