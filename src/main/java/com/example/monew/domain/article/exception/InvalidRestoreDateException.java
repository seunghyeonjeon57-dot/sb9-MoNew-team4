package com.example.monew.domain.article.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class InvalidRestoreDateException extends MonewException {

  public InvalidRestoreDateException(ErrorCode errorCode) {
    super(errorCode);
  }
}