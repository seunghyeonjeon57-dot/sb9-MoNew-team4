package com.example.monew.domain.notification.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class NotificationException extends MonewException {
  public NotificationException(ErrorCode errorCode) {
    super(errorCode);
  }
}