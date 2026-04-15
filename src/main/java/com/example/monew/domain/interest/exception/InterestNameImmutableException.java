package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;

public class InterestNameImmutableException extends MonewException {
  public InterestNameImmutableException() {
    super(ErrorCode.INTEREST_NAME_IMMUTABLE);
  }
}
