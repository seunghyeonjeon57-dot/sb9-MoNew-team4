package com.example.monew.domain.article.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class BackupFileNotFoundException extends MonewException {
  public BackupFileNotFoundException(ErrorCode errorCode) {
    super(errorCode);
  }
}
