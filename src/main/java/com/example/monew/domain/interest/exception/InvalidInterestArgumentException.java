package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class InvalidInterestArgumentException extends MonewException {

  public InvalidInterestArgumentException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
