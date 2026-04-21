package com.example.monew.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  // 공통 인프라
  INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청이 올바르지 않습니다."),
  MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),
  CONSTRAINT_VIOLATION(HttpStatus.BAD_REQUEST, "요청 파라미터 제약 조건을 위반했습니다."),
  MISSING_REQUEST_HEADER(HttpStatus.BAD_REQUEST, "필수 요청 헤더가 누락되었습니다."),
  METHOD_NOT_ALLOWED_REQUEST(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),
  UNSUPPORTED_MEDIA_TYPE_REQUEST(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),
  INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

  // Interest 도메인
  INTEREST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 관심사를 찾을 수 없습니다."),
  SIMILAR_INTEREST_NAME(HttpStatus.CONFLICT, "이미 80% 이상 유사한 관심사 이름이 존재합니다."),
  INTEREST_NAME_IMMUTABLE(HttpStatus.BAD_REQUEST, "관심사 이름은 수정할 수 없습니다."),
  INVALID_SORT_PARAMETER(HttpStatus.BAD_REQUEST, "허용되지 않은 정렬 파라미터입니다."),
  DUPLICATE_SUBSCRIPTION(HttpStatus.CONFLICT, "이미 해당 관심사를 구독 중입니다."),
  SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "구독 정보를 찾을 수 없습니다."),


  //Article 도메인
  ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 기사를 찾을 수 없습니다."),
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST,  "잘못된 입력 값입니다."),

  // User 도메인 (기존 보존)
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 유저를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다."),
  LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
  NICKNAME_BLANK(HttpStatus.BAD_REQUEST, "닉네임은 공백일 수 없습니다."),

  // Comment 도메인
  COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 댓글을 찾을 수 없습니다."),
  COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "댓글 작성자만 삭제할 수 있습니다."),
  COMMENT_CONTENT_BLANK(HttpStatus.BAD_REQUEST, "댓글 내용은 비어있을 수 없습니다."),
  DUPLICATE_LIKE(HttpStatus.CONFLICT, "이미 좋아요를 누른 댓글입니다."),
  LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요를 누르지 않은 댓글입니다."),
  // NOTIFICATION (도메인 추가)
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 알림을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;

  ErrorCode(HttpStatus status, String message) {
    this.status = status;
    this.message = message;
  }
}
