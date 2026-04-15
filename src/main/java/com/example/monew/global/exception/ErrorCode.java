package com.example.monew.global.exception;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  // 공통
  INVALID_REQUEST(BAD_REQUEST, "요청이 올바르지 않습니다."),
  MALFORMED_REQUEST_BODY(BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),
  CONSTRAINT_VIOLATION(BAD_REQUEST, "제약 조건 위반입니다."),
  MISSING_REQUEST_HEADER(BAD_REQUEST, "필수 요청 헤더가 누락되었습니다."),
  UNAUTHORIZED_REQUEST(UNAUTHORIZED, "인증이 필요합니다."),
  FORBIDDEN_REQUEST(FORBIDDEN, "접근 권한이 없습니다."),
  RESOURCE_NOT_FOUND(NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
  METHOD_NOT_ALLOWED_REQUEST(METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
  UNSUPPORTED_MEDIA_TYPE_REQUEST(UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
  INTERNAL_ERROR(INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

  // User
  USER_NOT_FOUND(NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(CONFLICT, "이미 사용 중인 이메일입니다."),
  INVALID_CREDENTIALS(UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

  // Interest
  INTEREST_NOT_FOUND(NOT_FOUND, "관심사를 찾을 수 없습니다."),
  SIMILAR_INTEREST_NAME(CONFLICT, "유사한 이름의 관심사가 이미 존재합니다."),
  INTEREST_NAME_IMMUTABLE(BAD_REQUEST, "관심사 이름은 변경할 수 없습니다."),
  INVALID_SORT_PARAMETER(BAD_REQUEST, "정렬 파라미터가 올바르지 않습니다."),
  DUPLICATE_SUBSCRIPTION(CONFLICT, "이미 구독 중인 관심사입니다."),
  SUBSCRIPTION_NOT_FOUND(NOT_FOUND, "구독 중이 아닌 관심사입니다."),

  // Article
  ARTICLE_NOT_FOUND(NOT_FOUND, "기사를 찾을 수 없습니다."),
  ARTICLE_FETCH_FAILED(BAD_GATEWAY, "외부 뉴스 수집에 실패했습니다."),
  DUPLICATE_ARTICLE_VIEW(CONFLICT, "이미 조회한 기사입니다."),

  // Comment
  COMMENT_NOT_FOUND(NOT_FOUND, "댓글을 찾을 수 없습니다."),
  COMMENT_FORBIDDEN(FORBIDDEN, "댓글에 대한 권한이 없습니다."),
  DUPLICATE_COMMENT_LIKE(CONFLICT, "이미 좋아요한 댓글입니다."),

  // Notification
  NOTIFICATION_NOT_FOUND(NOT_FOUND, "알림을 찾을 수 없습니다."),

  // Activity (MongoDB)
  USER_ACTIVITY_NOT_FOUND(NOT_FOUND, "활동 내역을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
