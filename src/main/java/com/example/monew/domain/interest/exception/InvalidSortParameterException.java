package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class InvalidSortParameterException extends MonewException {
  public InvalidSortParameterException(Map<String, Object> details) {
    super(ErrorCode.INVALID_SORT_PARAMETER, details);
  }
}
