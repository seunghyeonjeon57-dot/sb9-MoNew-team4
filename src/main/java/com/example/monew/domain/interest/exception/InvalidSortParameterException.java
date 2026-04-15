package com.example.monew.domain.interest.exception;

import com.example.monew.global.exception.ErrorCode;
import com.example.monew.global.exception.MonewException;
import java.util.Map;

public class InvalidSortParameterException extends MonewException {

  public InvalidSortParameterException(String parameter, String value, Object allowed) {
    super(ErrorCode.INVALID_SORT_PARAMETER, Map.of(
        "parameter", parameter,
        "value", String.valueOf(value),
        "allowed", allowed
    ));
  }
}
