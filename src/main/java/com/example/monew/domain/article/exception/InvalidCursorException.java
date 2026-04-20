package com.example.monew.domain.article.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class InvalidCursorException extends MonewException{

  public InvalidCursorException(ErrorCode errorCode) {
    super(errorCode);
  }
}
