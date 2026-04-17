package com.example.monew.domain.user.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class DuplicateEmailException extends MonewException {

  public DuplicateEmailException(String message) {
    super(ErrorCode.DUPLICATE_EMAIL);
  }
}
