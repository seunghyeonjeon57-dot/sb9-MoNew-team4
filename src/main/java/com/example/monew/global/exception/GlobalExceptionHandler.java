package com.example.monew.global.exception;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MonewException.class)
  public ResponseEntity<ErrorResponse> handleMonewException(MonewException ex) {
    ErrorCode code = ex.getErrorCode();
    return ResponseEntity.status(code.getStatus())
        .body(ErrorResponse.of(code, ex.getDetails()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> fieldErrors = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.put(fe.getField(), fe.getDefaultMessage());
    }
    Map<String, Object> details = new HashMap<>();
    details.put("fieldErrors", fieldErrors);
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
    Map<String, Object> details = Map.of("missingHeader", ex.getHeaderName());
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    Map<String, Object> details = Map.of("parameter", ex.getName(), "value", String.valueOf(ex.getValue()));
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getStatus())
        .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, details));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleFallback(Exception ex) {
    Map<String, Object> details = Map.of("exception", ex.getClass().getSimpleName());
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
        .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, details));
  }
}
