package com.example.monew.domain.user.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class UserNotFoundException extends MonewException {

  public UserNotFoundException(String message) {
    super(ErrorCode.USER_NOT_FOUND);
  }
}
