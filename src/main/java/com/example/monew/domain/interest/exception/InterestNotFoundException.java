package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class InterestNotFoundException extends MonewException {
  public InterestNotFoundException(Map<String, Object> details) {
    super(ErrorCode.INTEREST_NOT_FOUND, details);
  }
}
