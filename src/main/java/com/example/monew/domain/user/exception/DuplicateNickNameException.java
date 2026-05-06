package com.example.monew.domain.user.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class DuplicateNickNameException extends MonewException {

  public DuplicateNickNameException(String message) {
    super(ErrorCode.DUPLICATE_NICKNAME);
  }
}
