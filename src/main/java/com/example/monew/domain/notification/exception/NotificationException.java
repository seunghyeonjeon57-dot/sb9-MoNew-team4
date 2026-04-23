package com.example.monew.domain.notification.exception; // 패키지 경로는 프로젝트에 맞게!

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class NotificationException extends MonewException {
  public NotificationException(ErrorCode errorCode) {
    super(errorCode);
  }
}