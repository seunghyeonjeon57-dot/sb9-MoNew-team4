package com.example.monew.global.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  // 도메인 예외 — ErrorCode.status로 분기 (switch 제거)
  @ExceptionHandler(MonewException.class)
  public ResponseEntity<ErrorResponse> handleMonewException(MonewException e) {
    HttpStatus status = e.getErrorCode().getStatus();
    log.warn("도메인 예외: code={}, status={}, message={}",
        e.getErrorCode(), status.value(), e.getMessage());
    return ResponseEntity.status(status).body(new ErrorResponse(e, status.value()));
  }

  // V2 — Bean Validation 실패 (요청 본문 @Valid)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    Map<String, Object> validationErrors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String message = error.getDefaultMessage();
      validationErrors.put(fieldName, message);
    });
    log.warn("요청 유효성 검사 실패: {}", validationErrors);
    return respond(ErrorCode.INVALID_REQUEST, e, validationErrors);
  }

  // PathVariable / RequestParam 검증 실패
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
    Map<String, Object> violations = new HashMap<>();
    e.getConstraintViolations().forEach(v ->
        violations.put(v.getPropertyPath().toString(), v.getMessage()));
    log.warn("제약 조건 위반: {}", violations);
    return respond(ErrorCode.CONSTRAINT_VIOLATION, e, violations);
  }

  // V1 — 필수 요청 헤더 누락 (예: MoNew-Request-User-ID)
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException e) {
    Map<String, Object> details = new HashMap<>();
    details.put("header", e.getHeaderName());
    log.warn("필수 요청 헤더 누락: {}", e.getHeaderName());
    return respond(ErrorCode.MISSING_REQUEST_HEADER, e, details);
  }

  // 지원하지 않는 HTTP 메서드
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
    log.warn("지원하지 않는 HTTP 메서드: {}", e.getMethod());
    return respond(ErrorCode.METHOD_NOT_ALLOWED_REQUEST, e);
  }

  // 지원하지 않는 Content-Type
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
    log.warn("지원하지 않는 미디어 타입: {}", e.getContentType());
    return respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE_REQUEST, e);
  }

  // JSON 파싱 실패 / 요청 본문 누락
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e) {
    log.warn("요청 본문 파싱 실패: {}", e.getMessage());
    return respond(ErrorCode.MALFORMED_REQUEST_BODY, e);
  }

  // 매핑된 핸들러가 없는 URL (throw-exception-if-no-handler-found=true 전제)
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException e) {
    log.warn("경로 매칭 실패: {} {}", e.getHttpMethod(), e.getRequestURL());
    return respond(ErrorCode.RESOURCE_NOT_FOUND, e);
  }

  // 방어용 — 서비스 내부에서 비즈니스 예외가 아닌 IllegalArgumentException이 새는 경우
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
    log.warn("잘못된 인자: {}", e.getMessage());
    return respond(ErrorCode.INVALID_REQUEST, e);
  }

  // fallback
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("예상치 못한 오류: {}", e.getMessage(), e);
    return respond(ErrorCode.INTERNAL_ERROR, e);
  }

  private ResponseEntity<ErrorResponse> respond(ErrorCode code, Exception e) {
    return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, e));
  }

  private ResponseEntity<ErrorResponse> respond(ErrorCode code, Exception e, Map<String, Object> details) {
    return ResponseEntity.status(code.getStatus()).body(ErrorResponse.of(code, e, details));
  }
}
