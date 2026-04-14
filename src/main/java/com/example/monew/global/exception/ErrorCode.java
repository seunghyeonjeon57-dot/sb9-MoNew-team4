package com.example.monew.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  USER_NOT_FOUND("해당 유저를 찾을 수 없습니다.");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }
}
