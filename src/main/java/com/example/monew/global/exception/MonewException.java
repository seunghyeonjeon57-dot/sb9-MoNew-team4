package com.example.monew.global.exception;

import java.util.HashMap;
import java.util.Map;

public abstract class MonewException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> details;

  protected MonewException(ErrorCode errorCode) {
    this(errorCode, new HashMap<>());
  }

  protected MonewException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.details = details;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public Map<String, Object> getDetails() {
    return details;
  }
}
