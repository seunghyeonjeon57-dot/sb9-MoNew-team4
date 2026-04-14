package com.example.monew.global.exception;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    String code,
    String message,
    Map<String, Object> details,
    Instant timestamp
) {
  public static ErrorResponse of(ErrorCode errorCode, Map<String, Object> details) {
    return new ErrorResponse(errorCode.name(), errorCode.getMessage(), details, Instant.now());
  }

  public static ErrorResponse of(ErrorCode errorCode) {
    return of(errorCode, Map.of());
  }
}
