package com.example.monew.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심사를 찾을 수 없습니다."),
  SIMILAR_INTEREST_NAME(HttpStatus.CONFLICT, "유사한 이름의 관심사가 이미 존재합니다."),
  INTEREST_NAME_IMMUTABLE(HttpStatus.BAD_REQUEST, "관심사 이름은 변경할 수 없습니다."),

  DUPLICATE_SUBSCRIPTION(HttpStatus.CONFLICT, "이미 구독 중인 관심사입니다."),
  SUBSCRIPTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "구독 중이 아닌 관심사입니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }
}
