package com.example.monew.global.exception;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalException {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);
    ErrorResponse errorResponse = new ErrorResponse(e, INTERNAL_SERVER_ERROR.value());
    return ResponseEntity
        .status(INTERNAL_SERVER_ERROR)
        .body(errorResponse);
  }

  @ExceptionHandler(MonewException.class)
  public ResponseEntity<ErrorResponse> handleMonewException(MonewException exception) {
    log.error("커스텀 예외 발생: code={}, message={}",
        exception.getErrorCode(), exception.getMessage(), exception);
    return ResponseEntity
        .status(exception.getErrorCode().getStatus())
        .body(ErrorResponse.of(exception));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    log.warn("요청 유효성 검사 실패: {}", e.getMessage());

    Map<String, Object> fieldErrors = new HashMap<>();
    e.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      fieldErrors.put(fieldName, errorMessage);
    });

    return ResponseEntity
        .status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, fieldErrors));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(MissingRequestHeaderException e) {
    log.warn("필수 요청 헤더 누락: {}", e.getHeaderName());
    return ResponseEntity
        .status(ErrorCode.MISSING_REQUEST_HEADER.getStatus())
        .body(ErrorResponse.of(ErrorCode.MISSING_REQUEST_HEADER,
            Map.of("header", e.getHeaderName())));
  }
}
