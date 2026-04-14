package com.example.monew.global.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

@Getter
@RequiredArgsConstructor
public class ErrorResponse {
  private final Instant timestamp;
  private final String code;
  private final String message;
  private final Map<String, Object> details;
  private final String exceptionType;
  private final int status;
  private final String traceId;

  // 커스텀 예외
  public ErrorResponse(MonewException exception, int status) {
    this(Instant.now(),
        exception.getErrorCode().name(),
        exception.getMessage(),
        exception.getDetails(),
        exception.getClass().getSimpleName(),
        status,
        MDC.get("request_id")
    );
  }
  // 커스텀 예외로 해결하지 못한 예외
  public ErrorResponse(Exception exception, int status) {
    this(Instant.now(),
        exception.getClass().getSimpleName(),
        exception.getMessage(), new HashMap<>(),
        exception.getClass().getSimpleName(),
        status,
        MDC.get("request_id")
    );
  }
}
