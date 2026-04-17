package com.example.monew.domain.user.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class NickNameBlankException extends MonewException {

  public NickNameBlankException(String message) {
    super(ErrorCode.NICKNAME_BLANK);
  }
}
