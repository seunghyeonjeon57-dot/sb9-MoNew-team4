package com.example.monew.domain.user.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class LoginFailedException extends MonewException {

  public LoginFailedException(String message) {
    super(ErrorCode.LOGIN_FAILED);
  }
}
